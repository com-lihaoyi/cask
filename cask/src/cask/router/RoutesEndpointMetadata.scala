// package cask.router

// import cask.router.EntryPoint

// case class RoutesEndpointsMetadata[T](value: EndpointMetadata[T]*)
// object RoutesEndpointsMetadata{

//   implicit def initialize[T]: RoutesEndpointsMetadata[T] = ???
// }



package cask.router

import cask.router.EntryPoint

case class RoutesEndpointsMetadata[T](value: Seq[EndpointMetadata[T]])
object RoutesEndpointsMetadata{
  import scala.quoted._

  inline given initialize[T] as RoutesEndpointsMetadata[T] = ${initializeImpl}

  def initializeImpl[T](using qctx: QuoteContext, tpe: Type[T]): Expr[RoutesEndpointsMetadata[T]] = {
    import qctx.tasty._


    val routeParts: List[Expr[EndpointMetadata[T]]] = for {
      m <- tpe.unseal.symbol.methods
      annotations = m.annots.filter(_.tpe <:< typeOf[Decorator[_, _, _]])
      if (annotations.nonEmpty)
    } yield {

      if(!(annotations.last.tpe <:< typeOf[Endpoint[_, _, _]])) error(
        s"Last annotation applied to a function must be an instance of Endpoint, " +
          s"not ${annotations.last.tpe}",
        annotations.head.pos
      )
      val allEndpoints = annotations.filter(_.tpe <:< typeOf[Endpoint[_, _, _]])
      if(allEndpoints.length > 1) error(
        s"You can only apply one Endpoint annotation to a function, not " +
          s"${allEndpoints.length} in ${allEndpoints.map(_.tpe).mkString(", ")}",
        annotations.last.pos,
      )

      val decorators = annotations.reverse.map(_.seal.asInstanceOf[Expr[Decorator[_, _, _]]])

      '{

        val decos: List[Decorator[_, _, _]] = ${Expr.ofList(decorators)}
        val endpoint = decos.head.asInstanceOf[Endpoint[_, _, _]]

        val entrypoint = ${
          Macros.extractMethod[T](
            m,
            // (0 until annotations.length).toList.map{ i =>
            //   '{decos(${Expr(i)})}
            // }
            decorators
          )
        }

        EndpointMetadata(
          decos.drop(1), // TODO: check that decorator chains typecheck, i.e. replicate what seqify does in the macro
          endpoint, // endpoint, last decorator
          entrypoint
        )
      }

    }

    '{
      //${Expr.ofSeq(routeParts)}

      RoutesEndpointsMetadata[T](
        ${Expr.ofList(routeParts)}
      )
    }

  }

}
