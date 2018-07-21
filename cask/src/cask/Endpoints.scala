package cask

import cask.Router.EntryPoint
import io.undertow.server.HttpServerExchange
import collection.JavaConverters._

trait Endpoint[R]{
  val path: String
  def subpath: Boolean = false
  def wrapMethodOutput(t: R): Any
  def parseMethodInput[T](implicit p: ParamReader[T]) = p

  def handle(exchange: HttpServerExchange,
             remaining: Seq[String],
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Routes, (HttpServerExchange, Seq[String])]): Router.Result[Response]
}
trait WebEndpoint extends Endpoint[Response]{
  def wrapMethodOutput(t: Response) = t
  def handle(exchange: HttpServerExchange,
             remaining: Seq[String],
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Routes, (HttpServerExchange, Seq[String])]): Router.Result[Response] = {
    val allBindings =
      bindings.toSeq ++
        exchange.getQueryParameters
          .asScala
          .toSeq
          .flatMap{case (k, vs) => vs.asScala.map((k, _))}

    entryPoint.invoke(routes, (exchange, remaining), allBindings.map{case (k, v) => (k, Some(v))})
              .asInstanceOf[Router.Result[Response]]
  }
}
class get(val path: String, override val subpath: Boolean = false) extends WebEndpoint
class post(val path: String, override val subpath: Boolean = false) extends WebEndpoint
class put(val path: String, override val subpath: Boolean = false) extends WebEndpoint
class route(val path: String, val methods: Seq[String], override val subpath: Boolean = false) extends WebEndpoint
class static(val path: String) extends Endpoint[String] {
  override def subpath = true
  def wrapOutput(t: String) = t

  def wrapMethodOutput(t: String) = t

  def handle(exchange: HttpServerExchange,
             remaining: Seq[String],
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Routes, (HttpServerExchange, Seq[String])]): Router.Result[Response] = {
    entryPoint.invoke(routes, (exchange, remaining), Nil).asInstanceOf[Router.Result[String]] match{
      case Router.Result.Success(s) =>
        val relPath = java.nio.file.Paths.get(
          s + "/" + remaining.mkString("/")
        )
        if (java.nio.file.Files.exists(relPath) && java.nio.file.Files.isRegularFile(relPath)){
          Router.Result.Success(Response(java.nio.file.Files.newInputStream(relPath)))
        }else{
          Router.Result.Success(Response("", 404))
        }

      case e: Router.Result.Error => e

    }

  }
}
