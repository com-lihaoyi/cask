package cask.router

import scala.quoted.{ given, _ }

object Macros {

  /** Check that decorator inner and outer return types match.
    *
    * This replicates EndpointMetadata.seqify, but in a macro where error
    * positions can be controlled.
    */
  def checkDecorators(using qctx: Quotes)(decorators: List[Expr[Decorator[_, _, _]]]): Boolean = {
    import qctx.reflect._

    var hasErrors = false

    def check(prevOuter: TypeRepr, decorators: List[Expr[Decorator[_, _, _]]]): Unit =
      decorators match {
        case Nil =>
        case '{ $d: Decorator[outer, inner, _] } :: tail =>
          if (TypeRepr.of[inner] <:< prevOuter) {
            check(TypeRepr.of[outer], tail)
          } else {
            hasErrors = true
            report.error(
              s"required: cask.router.Decorator[_, ${prevOuter.show}, _]",
              d
            )
          }
        case _ => sys.error("internal error: expected only check decorators")
      }

    check(TypeRepr.of[Any], decorators)
    !hasErrors
  }

  /** Lookup default values for a method's parameters. */
  def getDefaultParams(using qctx: Quotes)(method: qctx.reflect.Symbol): Map[qctx.reflect.Symbol, Expr[Any]] = {
    import qctx.reflect._

    val params = method.paramSymss.flatten
    val defaults = collection.mutable.Map.empty[Symbol, Expr[Any]]

    val Name = (method.name + """\$default\$(\d+)""").r

    val idents = method.owner.tree.asInstanceOf[ClassDef].body
    idents.foreach{
      case deff @ DefDef(Name(idx), _, _, _, tree) =>
        val expr = Ref(deff.symbol).asExpr
        defaults += (params(idx.toInt - 1) -> expr)
      case _ =>
    }

    defaults.toMap
  }

  /** Summon the reader for a parameter. */
  def summonReader(using qctx: Quotes)(
    decorator: Expr[Decorator[_,_,_]],
    param: qctx.reflect.Symbol
  ): Expr[ArgReader[_, _, _]] = {
    import qctx.reflect._


    val paramTpt = param.tree.asInstanceOf[ValDef].tpt
    val inputReaderTypeRaw = Applied(
      TypeSelect(
        Term.of(decorator),
        "InputParser"
      ),
      List(paramTpt)
    ).tpe
    val inputReaderType = inputReaderTypeRaw.asType.asInstanceOf[quoted.Type[Any]]

    val reader = Expr.summon(using inputReaderType) match {
      case None =>
        report.error(
          s"no reader of type ${paramTpt.tpe.typeSymbol.fullName} found for parameter ${param.name}",
          param.pos.get
        )
        '{???}
      case Some(expr) => expr
    }
    reader.asInstanceOf[Expr[ArgReader[_,_,_]]]
  }

  /** Call a method given by its symbol.
    *
    * E.g.
    *
    * assuming:
    *
    *   def foo(x: Int, y: String)(z: Int)
    *
    *   val argss: List[List[Any]] = ???
    *
    * then:
    *
    *   call(<symbol of foo>, '{argss})
    *
    * will expand to:
    *
    *   foo(argss(0)(0), argss(0)(1))(argss(1)(0))
    *
    */
  def call(using qctx: Quotes)(
    method: qctx.reflect.Symbol,
    argss: Expr[Seq[Seq[Any]]]
  ): Expr[_] = {
    import qctx.reflect._
    val paramss = method.paramSymss

    if (paramss.isEmpty) {
      report.error("At least one parameter list must be declared.", method.pos.get)
      return '{???}
    }

    val fct = Ref(method)

    val accesses: List[List[Term]] = for (i <- paramss.indices.toList) yield {
      for (j <- paramss(i).indices.toList) yield {
        val t = paramss(i)(j).tree.asInstanceOf[ValDef].tpt.tpe.asType.asInstanceOf[Type[Any]]
        val e = '{
          $argss(${Expr(i)})(${Expr(j)}).asInstanceOf[$t]
        }
        Term.of(e)
      }
    }

    val base = Apply(fct, accesses.head)
    val application: Apply = accesses.tail.foldLeft(base)((lhs, args) => Apply(lhs, args))
    val expr = application.asExpr
    expr
  }

  /** Convert a result to an HTTP response
    *
    * Note: essentially, all this method does is summon a `cask.internal.Conversion`
    * and provide a helpful error message if it cannot be found. In this case,
    * one could wonder why we do the implicit summoning in this macro, rather than
    * emit "regular" code which does the summoning. The reason is to provide
    * helpful error messages with correct positions. We can control the position
    * in the macro, but if the error were to come from the expanded code the position
    * would be completely off.
    */
  def convertToResponse(using qctx: Quotes)(
    method: qctx.reflect.Symbol,
    endpoint: Expr[Endpoint[_, _, _]],
    result: Expr[Any]
  ): Expr[Any] = {
    import qctx.reflect._

    val innerReturnedTpt = TypeSelect(
      Term.of(endpoint),
      "InnerReturnedAlias"
    )

    val rtpt = method.tree.asInstanceOf[DefDef].returnTpt

    val conversionTpeRaw = Applied(
      TypeTree.of[cask.internal.Conversion],
      List(
        rtpt, innerReturnedTpt
      )
    ).tpe

    // the asInstanceOf is required to splice this back into an Expr; this is generally
    // unsafe, but we know that it will work in the context that this macro is invoked in
    val conversionTpe = conversionTpeRaw.asType.asInstanceOf[Type[Any]]

    val conversion = Expr.summon(using conversionTpe) match {
      case None =>
        report.error(s"can't convert ${rtpt.tpe.typeSymbol.fullName} to a response", method.pos.get)
        '{???}
      case Some(expr) => expr
    }

    '{
      $conversion.asInstanceOf[cask.internal.Conversion[Any, Any]].f($result)
    }
  }

  /** The type of paramters displayed in error messages */
  def friendlyName(using qctx: Quotes)(param: qctx.reflect.ValDef): String = {
    import qctx.reflect._

    // Note: manipulating strings here feels hacky. Maybe there is a better way?
    // We do it so that the name matches the name generated by the Scala 2 version,
    // so that tests can be shared across both versions.
    param.tpt.show
      .replaceAll("""scala\.Predef\.""", "")
      .replaceAll("""scala\.""", "")
  }

  def extractMethod[Cls](using qctx: Quotes, curCls: Type[Cls])(
    method: qctx.reflect.Symbol,
    decorators: List[Expr[Decorator[_, _, _]]], // these must also include the endpoint
    endpoint: Expr[Endpoint[_, _, _]]
  ): Expr[EntryPoint[Cls, cask.Request]] = {
    import qctx.reflect._

    val defaults = getDefaultParams(method)

    val exprs0 = for(idx <- method.paramSymss.indices) yield {
      val params: List[Symbol] = method.paramSymss(idx)

      // sometimes we have more params than annotated decorators, for example if
      // there are global decorators
      val decorator: Option[Expr[Decorator[_, _, _]]] = decorators.lift(idx)

      val exprs1 = for (param <- params) yield {
        val paramTree = param.tree.asInstanceOf[ValDef]
        val paramTpeName = friendlyName(paramTree)
        val paramTpe = paramTree.tpt.tpe.asType.asInstanceOf[Type[Any]]

        // The Scala 2 version uses a getter that takes as input an instance of
        // the current class.
        // Not sure why it's needed however, since we can actually access the
        // default parameter ident directly from the macro (hence we use a
        // wildcard function that simply discards its input and always returns
        // the default).
        val defaultGetter: Expr[Option[Cls => Any]] = defaults.get(param) match {
          case None => '{None}
          case Some(expr) =>
            '{Some((_: Cls) => $expr)}
        }

        val decoTpe = (decorator match {
          case Some(deco) =>
            TypeSelect(
              Term.of(deco),
              "InputTypeAlias"
            ).tpe.asType
          case None =>
            Type.of[Any]
        }).asInstanceOf[Type[Any]]

        val reader = decorator match {
          case Some(deco) => summonReader(deco, param)
          case None => '{ NoOpParser.instanceAny[$decoTpe] }
        }

        '{
          ArgSig[Any, Cls, Any, cask.Request](
            ${Expr(param.name)},
            ${Expr(paramTpeName)},
            doc = None, // TODO
            default = ${defaultGetter}
          )(using ${reader}.asInstanceOf[ArgReader[Any, Any, cask.Request]])
        }
      }
      Expr.ofList(exprs1)
    }
    val sigExprs = Expr.ofList(exprs0)
    '{
      EntryPoint[Cls, cask.Request](
        name = ${Expr(method.name)},
        argSignatures = $sigExprs,
        doc = None, // TODO
        invoke0 = (
          clazz: Cls,
          ctx: cask.Request,
          argss: Seq[Map[String, Any]],
          sigss: Seq[Seq[ArgSig[Any, _, _, cask.Request]]]
        ) => {
          val parsedArgss: Seq[Seq[Either[Seq[cask.router.Result.ParamError], Any]]] =
            sigss.zip(argss).map{ case (sigs, args) =>
              sigs.map{ case sig =>
                Runtime.makeReadCall(
                  args,
                  ctx,
                  (sig.default match {
                    case None => None
                    case Some(getter) =>
                      val value = getter.asInstanceOf[Cls => Any](clazz)
                      Some(value)
                  }),
                  sig
                )
              }
            }

          Runtime.validateLists(parsedArgss).map{ validated =>
            val result = ${call(using qctx)(method, '{validated})}

            ${
              convertToResponse(using qctx)(
                method,
                endpoint,
                '{result}
              )
            }
          }
        }
      )
    }

  }

}
