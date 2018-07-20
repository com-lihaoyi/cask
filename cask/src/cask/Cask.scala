package cask
import language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox.Context
class route(val path: String) extends StaticAnnotation

class Main(x: Any*)

object Server{
  case class Route(name: String, metadata: route)
  case class Routes[T](value: Route*)
  object Routes{
    implicit def initialize[T] = macro initializeImpl[T]
    implicit def initializeImpl[T: c.WeakTypeTag](c: Context): c.Expr[Routes[T]] = {
      import c.universe._

      val routes = c.weakTypeOf[T].members
        .map(m => (m, m.annotations.filter(_.tree.tpe =:= c.weakTypeOf[route])))
        .collect{case (m, Seq(a)) => (m, a)}

      val routeParts = for((m, a) <- routes) yield {
        val annotation = q"new ${a.tree.tpe}(..${a.tree.children.tail})"
        q"cask.Server.Route(${m.name.toString}, $annotation)"
      }
      c.Expr[Routes[T]](q"""cask.Server.Routes(..$routeParts)""")
    }
  }
}

class Server[T](){
  private[this] var routes0: Server.Routes[this.type] = null
  def routes =
    if (routes0 != null) routes0
    else throw new Exception("Routes not yet initialize")

  protected[this] def initialize()(implicit routes: Server.Routes[this.type]): Unit = {
    routes0 = routes
  }
}


