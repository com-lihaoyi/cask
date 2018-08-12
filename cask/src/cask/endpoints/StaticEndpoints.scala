package cask.endpoints

import cask.internal.Router
import cask.main.Endpoint
import cask.model.{Response, ParamContext}

class static(val path: String) extends Endpoint {
  type Output = String
  val methods = Seq("get")
  type Input = Seq[String]
  type InputParser[T] = QueryParamReader[T]
  override def subpath = true
  def wrapFunction(ctx: ParamContext, delegate: Delegate): Returned = {
    delegate(Map()).map(t => cask.model.Static(t + "/" + ctx.remaining.mkString("/")))
  }

  def wrapPathSegment(s: String): Input = Seq(s)
}
