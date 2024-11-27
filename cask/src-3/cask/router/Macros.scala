package cask.router

import scala.quoted.{ given, _ }

object Macros {

  /** Check that decorator inner and outer return types match.
    *
    * This replicates EndpointMetadata.seqify, but in a macro where error
    * positions can be controlled.
    */
  def checkDecorators(using Quotes)(decorators: List[Expr[Decorator[_, _, _, _]]]): Boolean = {
    import quotes.reflect._

    var hasErrors = false

    def check(prevOuter: TypeRepr, decorators: List[Expr[Decorator[_, _, _, _]]]): Unit =
      decorators match {
        case Nil =>
        case '{ $d: Decorator[outer, inner, _, _] } :: tail =>
          if (TypeRepr.of[inner] <:< prevOuter) {
            check(TypeRepr.of[outer], tail)
          } else {
            hasErrors = true
            report.error(
              s"required: cask.router.Decorator[_, ${prevOuter.show}, _, _]",
              d
            )
          }
        case _ => sys.error("internal error: expected only check decorators")
      }

    check(TypeRepr.of[Any], decorators)
    !hasErrors
  }

  /** Lookup default values for a method's parameters. */
  def getDefaultParams(using Quotes)(method: quotes.reflect.Symbol): Map[quotes.reflect.Symbol, Expr[Any]] = {
    import quotes.reflect._

    val params = method.paramSymss.flatten
    val defaults = collection.mutable.Map.empty[Symbol, Expr[Any]]

    val Name = (method.name + """\$default\$(\d+)""").r

    val idents = method.owner.tree.asInstanceOf[ClassDef].body
    idents.foreach{
      case deff @ DefDef(Name(idx), _, _, _) =>
        val expr = Ref(deff.symbol).asExpr
        defaults += (params(idx.toInt - 1) -> expr)
      case _ =>
    }

    defaults.toMap
  }

  /** Summon the reader for a parameter. */
  def summonReader(using Quotes)(
    decorator: Expr[Decorator[_,_,_,_]],
    param: quotes.reflect.Symbol
  ): Expr[ArgReader[_, _, _]] = {
    import quotes.reflect._


    val paramTpt = param.tree.asInstanceOf[ValDef].tpt
    val inputReaderTypeRaw = Applied(
      TypeSelect(
        decorator.asTerm,
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
  def call(using Quotes)(
    method: quotes.reflect.Symbol,
    argss: Expr[Seq[Seq[Any]]]
  ): Expr[_] = {
    import quotes.reflect._
    val paramss = method.paramSymss

    if (paramss.isEmpty) {
      report.error("At least one parameter list must be declared.", method.pos.get)
      return '{???}
    }

    val fct = Ref(method)

    val accesses: List[List[Term]] = for (i <- paramss.indices.toList) yield {
      for (j <- paramss(i).indices.toList) yield {
        val tpe = paramss(i)(j).tree.asInstanceOf[ValDef].tpt.tpe
        tpe.asType match
          case '[t] => '{ $argss(${Expr(i)})(${Expr(j)}).asInstanceOf[t] }.asTerm
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
  def convertToResponse(using Quotes)(
    method: quotes.reflect.Symbol,
    endpoint: Expr[Endpoint[_, _, _, _]],
    result: Expr[Any]
  ): Expr[Any] = {
    import quotes.reflect._

    val innerReturnedTpt = endpoint.asTerm.tpe.asType match {
      case '[Endpoint[_, innerReturned, _, _]] => TypeRepr.of[innerReturned]
      case _ => ???
    }

    val rtp = method.tree.asInstanceOf[DefDef].returnTpt.tpe

    val conversionTpe = TypeRepr.of[cask.internal.Conversion].appliedTo(
      List(rtp, innerReturnedTpt)
    )

    val conversion = Implicits.search(conversionTpe) match {
      case iss: ImplicitSearchSuccess =>
        iss.tree.asExpr
      case isf: ImplicitSearchFailure =>
        def prettyPos(pos: Position) =
          s"${pos.sourceFile}:${pos.startLine + 1}:${pos.startColumn + 1}"
        report.error(s"error in route definition `def ${method.name}` (at ${prettyPos(method.pos.get)}): the method's return type ${rtp.show} cannot be converted to the expected response type ${innerReturnedTpt.show}", method.pos.get)
        '{???}
    }

    '{
      $conversion.asInstanceOf[cask.internal.Conversion[Any, Any]].f($result)
    }
  }

  /** The type of paramters displayed in error messages */
  def friendlyName(using Quotes)(param: quotes.reflect.ValDef): String = {
    import quotes.reflect._

    // Note: manipulating strings here feels hacky. Maybe there is a better way?
    // We do it so that the name matches the name generated by the Scala 2 version,
    // so that tests can be shared across both versions.
    param.tpt.show
      .replaceAll("""scala\.Predef\.""", "")
      .replaceAll("""scala\.""", "")
  }

  def extractMethod[Cls: Type](using q: Quotes)(
    method: quotes.reflect.Symbol,
    decorators: List[Expr[Decorator[_, _, _, _]]], // these must also include the endpoint
    endpoint: Expr[Endpoint[_, _, _, _]]
  ): Expr[EntryPoint[Cls, Any]] = {
    import quotes.reflect._

    val defaults = getDefaultParams(method)

    val exprs0 = for(idx <- method.paramSymss.indices) yield {
      val params: List[Symbol] = method.paramSymss(idx)

      // sometimes we have more params than annotated decorators, for example if
      // there are global decorators
      val decorator: Option[Expr[Decorator[_, _, _, _]]] = decorators.lift(idx)

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

        def decoTpe = (decorator match {
          case Some(deco) =>
            TypeSelect(
              deco.asTerm,
              "InputTypeAlias"
            ).tpe.asType
          case None =>
            Type.of[Any]
        })

        val reader = decorator match {
          case Some(deco) => summonReader(deco, param)
          case None =>
            decoTpe match
              case '[t] => '{ NoOpParser.instanceAnyRequest[t] } // TODO
        }

        '{
          ArgSig[Any, Cls, Any, Any](
            ${Expr(param.name)},
            ${Expr(paramTpeName)},
            doc = None, // TODO
            default = ${defaultGetter}
          )(using ${reader}.asInstanceOf[ArgReader[Any, Any, Any]])
        }
      }
      Expr.ofList(exprs1)
    }
    val sigExprs = Expr.ofList(exprs0)

    '{
      EntryPoint[Cls, Any](
        name = ${Expr(method.name)},
        argSignatures = $sigExprs,
        doc = None, // TODO
        invoke0 = (
          clazz: Cls,
          ctxs: Seq[Any],
          argss: Seq[Map[String, Any]],
          sigss: Seq[Seq[ArgSig[Any, _, _, Any]]]
        ) => {
          val parsedArgss: Seq[Seq[Either[Seq[cask.router.Result.ParamError], Any]]] =
            sigss.lazyZip(argss).lazyZip(ctxs).map { case (sigs, args, ctx) =>
              sigs.map{ case sig =>
                Runtime.makeReadCall(
                  args,
                  ctx,
                  sig.default match {
                    case None => None
                    case Some(getter) =>
                      val value = getter.asInstanceOf[Cls => Any](clazz)
                      Some(value)
                  },
                  sig
                )
              }
            }

          Runtime.validateLists(parsedArgss).map{ validated =>
            val result = ${call(using q)(method, '{validated})}

            ${
              convertToResponse(using q)(
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
