package cask.main

import cask.internal.Router
import cask.internal.Router.EntryPoint
import cask.model.{BaseResponse, ParamContext}

import scala.reflect.macros.blackbox.Context
import language.experimental.macros

object Routes{

  trait Endpoint[R] extends Decorator{
    type InputType
    val path: String
    val methods: Seq[String]
    def subpath: Boolean = false
    def wrapMethodOutput(ctx: ParamContext,t: R): cask.internal.Router.Result[Any] = cask.internal.Router.Result.Success(t)
    def handle(ctx: ParamContext): Map[String, InputType]
    def wrapPathSegment(s: String): InputType
  }
  trait Decorator{
    type InputType
    def handle(ctx: ParamContext): Map[String, InputType]
  }

  case class EndpointMetadata[T](decorators: Seq[Decorator],
                                 endpoint: Endpoint[_],
                                 entryPoint: EntryPoint[T, ParamContext])
  case class RoutesEndpointsMetadata[T](value: EndpointMetadata[T]*)
  object RoutesEndpointsMetadata{
    implicit def initialize[T] = macro initializeImpl[T]
    implicit def initializeImpl[T: c.WeakTypeTag](c: Context): c.Expr[RoutesEndpointsMetadata[T]] = {
      import c.universe._
      val router = new cask.internal.Router[c.type](c)

      val routeParts = for{
        m <- c.weakTypeOf[T].members
        val annotations = m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[Decorator])
        if annotations.nonEmpty
      } yield {

        val annotObjects =
          for(annot <- annotations)
          yield q"new ${annot.tree.tpe}(..${annot.tree.children.tail})"
        val annotObjectSyms =
          for(_ <- annotations.indices)
          yield c.universe.TermName(c.freshName("annotObject"))
        val route = router.extractMethod(
          m.asInstanceOf[MethodSymbol],
          weakTypeOf[T],
          (t: router.c.universe.Tree) => q"${annotObjectSyms.last}.wrapMethodOutput(ctx, $t)",
          c.weakTypeOf[ParamContext],
          annotObjectSyms.map(annotObjectSym => q"$annotObjectSym.parseMethodInput"),
          annotObjectSyms.map(annotObjectSym => tq"$annotObjectSym.InputType")

        )

        val declarations =
          for((sym, obj) <- annotObjectSyms.zip(annotObjects))
          yield q"val $sym = $obj"

        val res = q"""{
          ..$declarations
          cask.main.Routes.EndpointMetadata(
            Seq(..${annotObjectSyms.dropRight(1)}),
            ${annotObjectSyms.last},
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
  private[this] var metadata0: Routes.RoutesEndpointsMetadata[this.type] = null
  def caskMetadata =
    if (metadata0 != null) metadata0
    else throw new Exception("Routes not yet initialize")

  protected[this] def initialize()(implicit routes: Routes.RoutesEndpointsMetadata[this.type]): Unit = {
    metadata0 = routes
  }
}

