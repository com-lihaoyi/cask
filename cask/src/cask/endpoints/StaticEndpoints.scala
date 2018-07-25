package cask.endpoints

import cask.internal.Router
import cask.internal.Router.EntryPoint
import cask.main.Routes
import cask.model.{BaseResponse, ParamContext}
import io.undertow.server.HttpServerExchange

class static(val path: String) extends Routes.Endpoint[String] {
  type InputType = Seq[String]
  override def subpath = true
  def wrapOutput(t: String) = t
  def parseMethodInput[T](implicit p: QueryParamReader[T]) = p
  def wrapMethodOutput(t: String) = t

  def handle(ctx: ParamContext,
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Seq[String], Routes, cask.model.ParamContext]): Router.Result[BaseResponse] = {
    entryPoint.invoke(routes, ctx, Map()).asInstanceOf[Router.Result[String]] match{
      case Router.Result.Success(s) =>
        Router.Result.Success(cask.model.Static(s + "/" + ctx.remaining.mkString("/")))

      case e: Router.Result.Error => e

    }

  }
}
