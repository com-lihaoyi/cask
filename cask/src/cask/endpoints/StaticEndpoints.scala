package cask.endpoints

import cask.internal.Router
import cask.main.Endpoint
import cask.model.ParamContext

class static(val path: String) extends Endpoint[String] {
  val methods = Seq("get")
  type Input = Seq[String]
  type InputParser[T] = QueryParamReader[T]
  override def subpath = true
  def wrapOutput(t: String) = t
  override def wrapMethodOutput(ctx: ParamContext, t: String) = {
    Router.Result.Success(cask.model.Static(t + "/" + ctx.remaining.mkString("/")))
  }

  def getRawParams(ctx: ParamContext) = Right(Map())
  def wrapPathSegment(s: String): Input = Seq(s)
}
