package cask.main

import cask.internal.Router.EntryPoint
import cask.model.Request

import scala.reflect.macros.blackbox.Context
import language.experimental.macros

object Routes{
  case class EndpointMetadata[T](decorators: Seq[Decorator],
                                 endpoint: BaseEndpoint,
                                 entryPoint: EntryPoint[T, _])
  case class RoutesEndpointsMetadata[T](value: EndpointMetadata[T]*)
  object RoutesEndpointsMetadata{
    implicit def initialize[T] = macro initializeImpl[T]
    implicit def initializeImpl[T: c.WeakTypeTag](c: Context): c.Expr[RoutesEndpointsMetadata[T]] = {
      import c.universe._
      val router = new cask.internal.Router[c.type](c)

      val routeParts = for{
        m <- c.weakTypeOf[T].members
        val annotations = m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[BaseDecorator]).reverse
        if annotations.nonEmpty
      } yield {
        if(!(annotations.head.tree.tpe <:< weakTypeOf[BaseEndpoint])) c.abort(
          annotations.head.tree.pos,
          s"Last annotation applied to a function must be an instance of Endpoint, " +
          s"not ${annotations.head.tree.tpe}"
        )
        val allEndpoints = annotations.filter(_.tree.tpe <:< weakTypeOf[BaseEndpoint])
        if(allEndpoints.length > 1) c.abort(
          annotations.head.tree.pos,
          s"You can only apply one Endpoint annotation to a function, not " +
          s"${allEndpoints.length} in ${allEndpoints.map(_.tree.tpe).mkString(", ")}"
        )
        val annotObjects =
          for(annot <- annotations)
          yield q"new ${annot.tree.tpe}(..${annot.tree.children.tail})"
        val annotObjectSyms =
          for(_ <- annotations.indices)
          yield c.universe.TermName(c.freshName("annotObject"))
        val route = router.extractMethod(
          m.asInstanceOf[MethodSymbol],
          weakTypeOf[T],
          q"${annotObjectSyms.head}.convertToResultType",
          tq"cask.Request",
          annotObjectSyms.map(annotObjectSym => q"$annotObjectSym.getParamParser"),
          annotObjectSyms.map(annotObjectSym => tq"$annotObjectSym.Input")

        )

        val declarations =
          for((sym, obj) <- annotObjectSyms.zip(annotObjects))
          yield q"val $sym = $obj"

        val res = q"""{
          ..$declarations
          cask.main.Routes.EndpointMetadata(
            Seq(..${annotObjectSyms.drop(1)}),
            ${annotObjectSyms.head},
            $route
          )
        }"""
        res
      }

      c.Expr[RoutesEndpointsMetadata[T]](q"""cask.main.Routes.RoutesEndpointsMetadata(..$routeParts)""")
    }
  }
}

trait Routes{

  def decorators = Seq.empty[cask.main.Decorator]
  private[this] var metadata0: Routes.RoutesEndpointsMetadata[this.type] = null
  def caskMetadata =
    if (metadata0 != null) metadata0
    else throw new Exception("Routes not yet initialize")

  protected[this] def initialize()(implicit routes: Routes.RoutesEndpointsMetadata[this.type]): Unit = {
    metadata0 = routes
  }
}

