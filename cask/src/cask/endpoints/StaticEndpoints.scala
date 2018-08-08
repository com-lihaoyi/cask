package cask.endpoints

import cask.internal.Router
import cask.main.Endpoint
import cask.model.{Response, ParamContext}

class static(val path: String) extends Endpoint[String] {
  val methods = Seq("get")
  type Input = Seq[String]
  type InputParser[T] = QueryParamReader[T]
  override def subpath = true
  def wrapOutput(t: String) = t

  def wrapMethodOutput(ctx: ParamContext,
                       delegate: Map[String, Input] => Router.Result[String]): Router.Result[Response] = {
    delegate(Map()) match{
      case Router.Result.Success(t) => Router.Result.Success(cask.model.Static(t + "/" + ctx.remaining.mkString("/")))
      case e: Router.Result.Error => e
    }
  }

  def wrapPathSegment(s: String): Input = Seq(s)
}
