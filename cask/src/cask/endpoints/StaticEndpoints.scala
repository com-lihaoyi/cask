package cask.endpoints

import cask.internal.Router
import cask.main.Routes
import cask.model.ParamContext

class static(val path: String) extends Routes.Endpoint[String] {
  val methods = Seq("get")
  type InputType = Seq[String]
  override def subpath = true
  def wrapOutput(t: String) = t
  def parseMethodInput[T](implicit p: QueryParamReader[T]) = p
  override def wrapMethodOutput(ctx: ParamContext, t: String) = {
    Router.Result.Success(cask.model.Static(t + "/" + ctx.remaining.mkString("/")))
  }

  def handle(ctx: ParamContext) = Map()
  def wrapPathSegment(s: String): InputType = Seq(s)
}
