package test.cask

import cask.FormValue
import io.undertow.server.HttpServerExchange

object FormJsonPost extends cask.MainRoutes{
  @cask.postJson("/json")
  def jsonEndpoint(x: HttpServerExchange, value1: ujson.Js.Value, value2: Seq[Int]) = {
    "OK " + value1 + " " + value2
  }

  @cask.postForm("/form")
  def formEndpoint(x: HttpServerExchange, value1: FormValue, value2: Seq[Int]) = {
    "OK " + value1 + " " + value2
  }

  initialize()
}

