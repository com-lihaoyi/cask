package cask.endpoints

import cask.internal.Router
import cask.main.{Endpoint, HttpDecorator}
import cask.model.{ParamContext, Response}

class static(val path: String) extends Endpoint with HttpDecorator{
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
