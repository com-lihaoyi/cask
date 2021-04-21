package cask.router

import cask.router.EntryPoint

case class RoutesEndpointsMetadata[T](value: Seq[EndpointMetadata[T]])
object RoutesEndpointsMetadata{
  import scala.quoted._

  inline given initialize[T]: RoutesEndpointsMetadata[T] = ${initializeImpl}

  def setRoutesImpl[T: Type](setter: Expr[RoutesEndpointsMetadata[T] => Unit])(using Quotes): Expr[Unit] = {
    '{
      $setter(${initializeImpl[T]})
      ()
    }
  }

  def initializeImpl[T: Type](using q: Quotes): Expr[RoutesEndpointsMetadata[T]] = {
    import quotes.reflect._

    val routeParts: List[Expr[EndpointMetadata[T]]] = for {
      m <- TypeRepr.of[T].typeSymbol.memberMethods
      annotations = m.annotations.filter(_.tpe <:< TypeRepr.of[Decorator[_, _, _]])
      if (annotations.nonEmpty)
    } yield {

      if(!(annotations.head.tpe <:< TypeRepr.of[Endpoint[_, _, _]])) {
        report.error(s"Last annotation applied to a function must be an instance of Endpoint, " +
          s"not ${annotations.head.tpe.show}",
          annotations.head.pos
        )
        return '{???} // in this case, we can't continue expansion of this macro
      }
      val allEndpoints = annotations.filter(_.tpe <:< TypeRepr.of[Endpoint[_, _, _]])
      if(allEndpoints.length > 1) {
        report.error(
          s"You can only apply one Endpoint annotation to a function, not " +
            s"${allEndpoints.length} in ${allEndpoints.map(_.tpe.show).mkString(", ")}",
          annotations.last.pos,
        )
        return '{???}
      }

      val decorators = annotations.map(_.asExprOf[Decorator[_, _, _]])

      if (!Macros.checkDecorators(decorators))
        return '{???} // there was a type mismatch in the decorator chain

      val endpointExpr = decorators.head.asExprOf[Endpoint[_, _, _]]
      val entrypointExpr = Macros.extractMethod[T](m, decorators, endpointExpr)

      '{
        val entrypoint: EntryPoint[T, cask.Request] = ${entrypointExpr}

        EndpointMetadata[T](
          // the Scala 2 version and non-macro code expects decorators to be reversed
          ${Expr.ofList(decorators.drop(1).reverse)},
          ${endpointExpr},
          entrypoint
        )
      }

    }

    '{
      RoutesEndpointsMetadata[T](
        ${Expr.ofList(routeParts)}
      )
    }

  }

}
