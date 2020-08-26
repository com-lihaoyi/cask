package cask.router

import cask.router.EntryPoint

case class RoutesEndpointsMetadata[T](value: Seq[EndpointMetadata[T]])
object RoutesEndpointsMetadata{
  import scala.quoted._

  inline given initialize[T] as RoutesEndpointsMetadata[T] = ${initializeImpl}

  def setRoutesImpl[T](setter: Expr[RoutesEndpointsMetadata[T] => Unit])
    (using qctx: QuoteContext, tpe: Type[T]): Expr[Unit] = {
    '{
      $setter(${initializeImpl[T]})
      ()
    }
  }

  def initializeImpl[T](using qctx: QuoteContext, tpe: Type[T]): Expr[RoutesEndpointsMetadata[T]] = {
    import qctx.tasty._

    val routeParts: List[Expr[EndpointMetadata[T]]] = for {
      m <- tpe.unseal.symbol.methods
      annotations = m.annots.filter(_.tpe <:< typeOf[Decorator[_, _, _]])
      if (annotations.nonEmpty)
    } yield {

      if(!(annotations.head.tpe <:< typeOf[Endpoint[_, _, _]])) {
        error(s"Last annotation applied to a function must be an instance of Endpoint, " +
          s"not ${annotations.head.tpe.show}",
          annotations.head.pos
        )
        return '{???} // in this case, we can't continue expansion of this macro
      }
      val allEndpoints = annotations.filter(_.tpe <:< typeOf[Endpoint[_, _, _]])
      if(allEndpoints.length > 1) error(
        s"You can only apply one Endpoint annotation to a function, not " +
          s"${allEndpoints.length} in ${allEndpoints.map(_.tpe.show).mkString(", ")}",
        annotations.last.pos,
      )

      val decorators = annotations.map(_.seal.asInstanceOf[Expr[Decorator[_, _, _]]])
      Macros.checkDecorators(decorators)
      val endpoint = decorators.head.asInstanceOf[Expr[Endpoint[_, _, _]]]

      '{

        val entrypoint: EntryPoint[T, cask.Request] = ${
          Macros.extractMethod[T](
            m,
            decorators,
            endpoint
          )
        }

        EndpointMetadata[T](
          // the Scala 2 version and non-macro code expects decorators to be reversed
          ${Expr.ofList(decorators.drop(1).reverse)},
          ${endpoint},
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
