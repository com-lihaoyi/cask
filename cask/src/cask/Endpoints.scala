package cask

import cask.Router.EntryPoint
import io.undertow.server.HttpServerExchange

import collection.JavaConverters._

trait Endpoint[R]{
  type InputType
  val path: String
  def subpath: Boolean = false
  def wrapMethodOutput(t: R): Any
  def handle(exchange: HttpServerExchange,
             remaining: Seq[String],
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[InputType, Routes, (HttpServerExchange, Seq[String])]): Router.Result[BaseResponse]
}
trait WebEndpoint extends Endpoint[Response]{
  type InputType = Seq[String]
  def wrapMethodOutput(t: Response) = t
  def parseMethodInput[T](implicit p: QueryParamReader[T]) = p
  def handle(exchange: HttpServerExchange,
             remaining: Seq[String],
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Seq[String], Routes, (HttpServerExchange, Seq[String])]): Router.Result[Response] = {
    val allBindings =
      bindings.map{case (k, v) => (k, Seq(v))} ++
      exchange.getQueryParameters
        .asScala
        .toSeq
        .map{case (k, vs) => (k, vs.asScala.toSeq)}

    entryPoint.invoke(routes, (exchange, remaining), allBindings)
              .asInstanceOf[Router.Result[Response]]
  }
}
class get(val path: String, override val subpath: Boolean = false) extends WebEndpoint
class post(val path: String, override val subpath: Boolean = false) extends WebEndpoint
class put(val path: String, override val subpath: Boolean = false) extends WebEndpoint
class route(val path: String, val methods: Seq[String], override val subpath: Boolean = false) extends WebEndpoint
class static(val path: String) extends Endpoint[String] {
  type InputType = Seq[String]
  override def subpath = true
  def wrapOutput(t: String) = t
  def parseMethodInput[T](implicit p: QueryParamReader[T]) = p
  def wrapMethodOutput(t: String) = t

  def handle(exchange: HttpServerExchange,
             remaining: Seq[String],
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Seq[String], Routes, (HttpServerExchange, Seq[String])]): Router.Result[Response] = {
    entryPoint.invoke(routes, (exchange, remaining), Map()).asInstanceOf[Router.Result[String]] match{
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