package app

import cask.main.HttpDecorator
import cask.model.ParamContext


class custom(val path: String, val methods: Seq[String]) extends cask.Endpoint with HttpDecorator{
  type Output = Int
  def wrapFunction(ctx: cask.ParamContext, delegate: Delegate): Returned = {
    delegate(Map()).map{num =>
      cask.Response("Echo " + num, statusCode = num)
    }
  }

  def wrapPathSegment(s: String) = Seq(s)

  type Input = Seq[String]
  type InputParser[T] = cask.endpoints.QueryParamReader[T]
}

object Endpoints extends cask.MainRoutes{


  @custom("/echo/:status", methods = Seq("get"))
  def echoStatus(status: String) = {
    status.toInt
  }

  initialize()
}
