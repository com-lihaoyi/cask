package cask.endpoints

import cask.internal.Router
import cask.internal.Router.EntryPoint
import cask.main.Routes
import cask.model.BaseResponse
import io.undertow.server.HttpServerExchange

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
             entryPoint: EntryPoint[Seq[String], Routes, (HttpServerExchange, Seq[String])]): Router.Result[BaseResponse] = {
    entryPoint.invoke(routes, (exchange, remaining), Map()).asInstanceOf[Router.Result[String]] match{
      case Router.Result.Success(s) =>
        Router.Result.Success(cask.model.Static(s + "/" + remaining.mkString("/")))

      case e: Router.Result.Error => e

    }

  }
}
