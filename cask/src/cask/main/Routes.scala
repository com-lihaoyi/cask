package cask.main

import cask.internal.Router.{ArgReader, EntryPoint}
import cask.model.ParamContext

import scala.reflect.macros.blackbox.Context
import language.experimental.macros

object Routes{

  trait Endpoint[R] extends BaseDecorator{

    val path: String
    val methods: Seq[String]
    def subpath: Boolean = false
    def wrapMethodOutput(ctx: ParamContext,t: R): cask.internal.Router.Result[Any] = {
      cask.internal.Router.Result.Success(t)
    }

    def wrapPathSegment(s: String): Input

  }

  /**
    * The core interface of decorator annotations: the decorator provides "raw"
    * values to the annotated function via `getRawParams`, which then get
    * processed by `getParamParser` into the correct argument types before
    * being passed to the function.
    *
    * For a trivial "provide value" decorator, `getRawParams` would return the
    * final param value and `getParamParser` would return a no-op parser. For
    * a decorator that takes its input as query-params, JSON, or similar,
    * `getRawParams` would provide raw query/JSON/etc. values and
    * `getParamParser` would be responsible for processing those into the
    * correct parameter types.
    */
  trait BaseDecorator{
    type Input
    type InputParser[T] <: ArgReader[Input, T, ParamContext]
    def getRawParams(ctx: ParamContext): Either[cask.model.Response, Map[String, Input]]
    def getParamParser[T](implicit p: InputParser[T]) = p

  }

  trait Decorator extends BaseDecorator {
    type Input = Any
    type InputParser[T] = NoOpParser[Input, T]
  }

  class NoOpParser[Input, T] extends ArgReader[Input, T, ParamContext] {
    def arity = 1

    def read(ctx: ParamContext, label: String, input: Input) = input.asInstanceOf[T]
  }
  object NoOpParser{
    implicit def instance[Input, T] = new NoOpParser[Input, T]
  }

  case class EndpointMetadata[T](decorators: Seq[BaseDecorator],
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
        val annotations = m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[BaseDecorator]).reverse
        if annotations.nonEmpty
      } yield {
        if(!(annotations.head.tree.tpe <:< weakTypeOf[Endpoint[_]])) c.abort(
          annotations.head.tree.pos,
          s"Last annotation applied to a function must be an instance of Endpoint, " +
          s"not ${annotations.head.tree.tpe}"
        )
        val allEndpoints = annotations.filter(_.tree.tpe <:< weakTypeOf[Endpoint[_]])
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
          (t: router.c.universe.Tree) => q"${annotObjectSyms.head}.wrapMethodOutput(ctx, $t)",
          c.weakTypeOf[ParamContext],
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
  private[this] var metadata0: Routes.RoutesEndpointsMetadata[this.type] = null
  def caskMetadata =
    if (metadata0 != null) metadata0
    else throw new Exception("Routes not yet initialize")

  protected[this] def initialize()(implicit routes: Routes.RoutesEndpointsMetadata[this.type]): Unit = {
    metadata0 = routes
  }
}

