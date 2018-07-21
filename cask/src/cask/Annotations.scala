package cask

import cask.Router.EntryPoint
import io.undertow.server.HttpServerExchange
import collection.JavaConverters._

trait EndpointAnnotation[R]{
  val path: String
  def wrapMethodOutput(t: R): Any
  def parseMethodInput[T](implicit p: ParamReader[T]) = p

  def handle(exchange: HttpServerExchange,
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Routes, HttpServerExchange]): Router.Result[Response]
}
trait RouteBase extends EndpointAnnotation[Response]{
  def wrapMethodOutput(t: Response) = t
  def handle(exchange: HttpServerExchange,
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Routes, HttpServerExchange]): Router.Result[Response] = {
    val allBindings =
      bindings.toSeq ++
        exchange.getQueryParameters
          .asScala
          .toSeq
          .flatMap{case (k, vs) => vs.asScala.map((k, _))}

    entryPoint.invoke(routes, exchange, allBindings.map{case (k, v) => (k, Some(v))})
              .asInstanceOf[Router.Result[Response]]
  }
}
class get(val path: String) extends RouteBase
class post(val path: String) extends RouteBase
class put(val path: String) extends RouteBase
class route(val path: String, val methods: Seq[String]) extends RouteBase
class static(val path: String) extends EndpointAnnotation[String] {
  def wrapOutput(t: String) = t

  def wrapMethodOutput(t: String) = t

  def handle(exchange: HttpServerExchange,
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Routes, HttpServerExchange]): Router.Result[Response] = {
    entryPoint.invoke(routes, exchange, Nil).asInstanceOf[Router.Result[String]] match{
      case Router.Result.Success(s) =>
        println("XXX STATIC")
        val relPath = java.nio.file.Paths.get(
          s + Util.splitPath(exchange.getRequestPath).drop(Util.splitPath(path).length).mkString("/")
        )
        println("Y")
        if (java.nio.file.Files.exists(relPath) && java.nio.file.Files.isRegularFile(relPath)){
          Router.Result.Success(Response(java.nio.file.Files.newInputStream(relPath)))
        }else{
          Router.Result.Success(Response("", 404))
        }

      case e: Router.Result.Error =>
        println("XXX")
        e

    }

  }
}
