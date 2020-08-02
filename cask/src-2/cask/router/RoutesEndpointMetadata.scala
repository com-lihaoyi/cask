package cask.router

import cask.router.EntryPoint

import language.experimental.macros
import scala.reflect.macros.blackbox
case class EndpointMetadata[T](decorators: Seq[Decorator[_, _, _]],
                               endpoint: Endpoint[_, _, _],
                               entryPoint: EntryPoint[T, _])
object EndpointMetadata{
  // `seqify` is used to statically check that the decorators applied to each
  // individual endpoint method line up, and each decorator's `OuterReturned`
  // correctly matches the enclosing decorator's `InnerReturned`. We don't bother
  // checking decorators defined as part of cask.Main or cask.Routes, since those
  // are both more dynamic (and hard to check) and also less often used and thus
  // less error prone
  def seqify1(d: Decorator[_, _, _]) = Seq(d)
  def seqify2[T1]
             (d1: Decorator[T1, _, _])
             (d2: Decorator[_, T1, _]) = Seq(d1, d2)
  def seqify3[T1, T2]
             (d1: Decorator[T1, _, _])
             (d2: Decorator[T2, T1, _])
             (d3: Decorator[_, T2, _]) = Seq(d1, d2, d3)
  def seqify4[T1, T2, T3]
             (d1: Decorator[T1, _, _])
             (d2: Decorator[T2, T1, _])
             (d3: Decorator[T3, T2, _])
             (d4: Decorator[_, T3, _]) = Seq(d1, d2, d3, d4)
  def seqify5[T1, T2, T3, T4]
             (d1: Decorator[T1, _, _])
             (d2: Decorator[T2, T1, _])
             (d3: Decorator[T3, T2, _])
             (d4: Decorator[T4, T3, _])
             (d5: Decorator[_, T4, _]) = Seq(d1, d2, d3, d4, d5)
  def seqify6[T1, T2, T3, T4, T5]
             (d1: Decorator[T1, _, _])
             (d2: Decorator[T2, T1, _])
             (d3: Decorator[T3, T2, _])
             (d4: Decorator[T4, T3, _])
             (d5: Decorator[T5, T4, _])
             (d6: Decorator[_, T5, _]) = Seq(d1, d2, d3, d4)
}
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

      val annotPositions =
        for(a <- annotations)
        yield a.tree.find(_.pos != NoPosition) match{
          case None => m.pos
          case Some(t) => t.pos
        }

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

      val seqify = TermName("seqify" + annotObjectSyms.length)

      val seqifyCall = annotObjectSyms
        .zip(annotPositions)
        .reverse
        .foldLeft[Tree](q"cask.router.EndpointMetadata.$seqify"){
          case (lhs, (rhs, pos)) => q"$lhs(${c.internal.setPos(q"$rhs", pos)})"
        }

      q"""{
        ..$declarations
        cask.router.EndpointMetadata(
          $seqifyCall.reverse.dropRight(1),
          ${annotObjectSyms.last},
          $route
        )
      }"""
    }

    c.Expr[RoutesEndpointsMetadata[T]](q"""cask.router.RoutesEndpointsMetadata(..$routeParts)""")
  }
}