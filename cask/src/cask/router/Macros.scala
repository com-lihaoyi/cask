package cask.router

import scala.quoted.{ given _, _ }

object Macros {

  def getDefaultParams(using qctx: QuoteContext)(method: qctx.tasty.Symbol): Seq[Option[Expr[Any]]] = {
    import qctx.tasty._

    val params = method.paramSymss.flatten
    val defaults = Array.fill[Option[Expr[Any]]](params.length)(None)

    val Name = (method.name + """\$default\$(\d+)""").r

    val idents = method.owner.tree.asInstanceOf[ClassDef].body
    idents.foreach{
      case deff @ DefDef(Name(idx), _, _, _, tree) =>
        val expr = Ref(deff.symbol).seal
        defaults(idx.toInt - 1) = Some(expr)
      case _ =>
    }

    defaults
  }

  def findReader(using qctx: QuoteContext)(
    decorator: Expr[Decorator[_,_,_]],
    param: qctx.tasty.Symbol
  ): Expr[ArgReader[_, _, _]] = {
    import qctx.tasty._


    val paramTpt = param.tree.asInstanceOf[ValDef].tpt
    val inputReaderTypeRaw = Applied(
      TypeSelect(
        decorator.unseal,
        "InputParser"
      ),
      List(paramTpt)
    ).tpe
    val inputReaderType = inputReaderTypeRaw.seal.asInstanceOf[quoted.Type[Any]]

    val reader = Expr.summon(using inputReaderType) match {
      case None =>
        error(
          s"no reader of type ${paramTpt.tpe.typeSymbol.fullName} found for parameter ${param.name}",
          param.pos
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
  def call(using qctx: QuoteContext)(
    method: qctx.tasty.Symbol,
    argss: Expr[Seq[Seq[Any]]]
  ): Expr[_] = {
    import qctx.tasty._
    val paramss = method.paramSymss

    if (paramss.isEmpty) {
      error("At least one parameter list must be declared.", method.pos)
      return '{???}
    }

    def get(i: Int, j: Int) = '{ $argss(${Expr(i)})(${Expr(j)}) }

    val fct = Ref(method)

    val accesses: List[List[Term]] = for (i <- paramss.indices.toList) yield {
      for (j <- paramss(i).indices.toList) yield {
        get(i, j).unseal
      }
    }


    val base = Apply(fct, accesses.head)
    val application: Apply = accesses.tail.foldLeft(base)((lhs, args) => Apply(lhs, args))
    val expr = application.seal
    expr
  }

  /** Convert a result to an HTTP response */
  def convertToResponse(using qctx: QuoteContext)(
    method: qctx.tasty.Symbol,
    decorators: Seq[Expr[Decorator[_, _, _]]],
    result: Expr[Any]
  ): Expr[Any] = {
    import qctx.tasty._

    val endpoint = decorators.head.unseal

    if (!(endpoint.tpe <:< typeOf[Endpoint[_, _, _]])) {
      error("the last decorator must be a cask.router.Endpoint", endpoint.pos)
      return '{???}
    }

    val innerReturnedTpt = TypeSelect(
      endpoint,
      "InnerReturnedAlias"
    )

    val rtpt = method.tree.asInstanceOf[DefDef].returnTpt

    val conversionTpeRaw = Applied(
      '[cask.internal.Conversion].unseal,
      List(
        rtpt, innerReturnedTpt
      )
    ).tpe
    // the asInstanceOf is required to splice this back into an Expr; this is generally
    // unsafe, but we know that it will work in the context that this macro is invoked in
    val conversionTpe = conversionTpeRaw.seal.asInstanceOf[quoted.Type[Any]]

    val conversion = Expr.summon(using conversionTpe) match {
      case None =>
        error(s"can't convert ${rtpt.tpe.typeSymbol.fullName} to a response", method.pos)
        '{???}
      case Some(expr) => expr
    }

    '{
      $conversion.asInstanceOf[cask.internal.Conversion[Any, Any]].f($result)
    }
  }

  def extractMethod[Cls](using qctx: QuoteContext, curCls: Type[Cls])(
    method: qctx.tasty.Symbol,
    decorators: List[Expr[Decorator[_, _, _]]]
  ): Expr[EntryPoint[Cls, cask.Request]] = {
    import qctx.tasty._


    val exprs0 = for(idx <- method.paramSymss.indices) yield {
      val params: List[Symbol] = method.paramSymss(idx)
      val decorator: Expr[Decorator[_, _, _]] = decorators(idx)

      val exprs1 = for (param <- params) yield {
        val paramTree = param.tree.asInstanceOf[ValDef]
        val paramTpeName = paramTree.tpt.tpe.typeSymbol.fullName
        val paramTpe = paramTree.tpt.tpe.seal.asInstanceOf[quoted.Type[Any]]

        '{
          val deco = ${decorator}

          val reader = ${findReader(decorator, param) }.asInstanceOf[ArgReader[deco.InputTypeAlias, $paramTpe, cask.Request]]

          ArgSig[deco.InputTypeAlias, Cls, $paramTpe, cask.Request](
            ${Expr(param.name)},
            ${Expr(paramTpeName)},
            doc = None, // TODO
            default = None // TODO
          )(using reader)
        }
      }
      Expr.ofList(exprs1)
    }
    val sigExprs = Expr.ofList(exprs0)

    '{
      val sigs = $sigExprs
      EntryPoint[Cls, cask.Request](
        name = ${Expr(method.name)},
        argSignatures = sigs,
        doc = None, // TODO
        invoke0 = (
          clazz: Cls,
          ctx: cask.Request,
          argss: Seq[Map[String, Any]],
          sigss: Seq[Seq[ArgSig[Any, _, _, cask.Request]]]
        ) => {
          val parsedArgss: Seq[Seq[Either[Seq[cask.router.Result.ParamError], Any]]] =
            for ( (sigs, args) <- sigss.zip(argss)) yield {
              for (sig <- sigs) yield {
                Runtime.makeReadCall(
                  args,
                  ctx,
                  None, // default, TODO
                  sig
                )
              }
            }
          Runtime.validateLists(parsedArgss).map{ validated =>
            val result = ${call(method, '{validated})}

            ${
              convertToResponse(
                method,
                decorators,
                '{result}
              )
            }
          }
        }
      )
    }

  }


}
