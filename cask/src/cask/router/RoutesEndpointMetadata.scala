package cask.router

import cask.router.EntryPoint

import language.experimental.macros
import scala.reflect.macros.blackbox
case class EndpointMetadata[T](decorators: Seq[Decorator[_, _, _]],
                               endpoint: Endpoint[_, _, _],
                               entryPoint: EntryPoint[T, _])
case class RoutesEndpointsMetadata[T](value: EndpointMetadata[T]*)
object RoutesEndpointsMetadata{
  implicit def initialize[T]: RoutesEndpointsMetadata[T] = macro initializeImpl[T]
  implicit def initializeImpl[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[RoutesEndpointsMetadata[T]] = {
    import c.universe._
    val router = new cask.router.Macros[c.type](c)

    val routeParts = for{
      m <- c.weakTypeOf[T].members
      annotations = m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[Decorator[_, _, _]])
      if annotations.nonEmpty
    } yield {
      if(!(annotations.last.tree.tpe <:< weakTypeOf[Endpoint[_, _, _]])) c.abort(
        annotations.head.tree.pos,
        s"Last annotation applied to a function must be an instance of Endpoint, " +
          s"not ${annotations.last.tree.tpe}"
      )
      val allEndpoints = annotations.filter(_.tree.tpe <:< weakTypeOf[Endpoint[_, _, _]])
      if(allEndpoints.length > 1) c.abort(
        annotations.last.tree.pos,
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
        q"${annotObjectSyms.last}.convertToResultType",
        tq"cask.Request",
        annotObjectSyms.reverse.map(annotObjectSym => q"$annotObjectSym.getParamParser"),
        annotObjectSyms.reverse.map(annotObjectSym => tq"$annotObjectSym.InputTypeAlias")
      )

      val declarations =
        for((sym, obj) <- annotObjectSyms.zip(annotObjects))
          yield q"val $sym = $obj"

      val res = q"""{
          ..$declarations
          cask.router.EndpointMetadata(
            Seq(..${annotObjectSyms.dropRight(1)}),
            ${annotObjectSyms.last},
            $route
          )
        }"""
      res
    }

    c.Expr[RoutesEndpointsMetadata[T]](q"""cask.router.RoutesEndpointsMetadata(..$routeParts)""")
  }
}