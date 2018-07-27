package test.cask

import cask.FormValue

object FormJsonPost extends cask.MainRoutes{
  @cask.postJson("/json")
  def jsonEndpoint(value1: ujson.Js.Value, value2: Seq[Int]) = {
    "OK " + value1 + " " + value2
  }

  @cask.postForm("/form")
  def formEndpoint(value1: FormValue, value2: Seq[Int]) = {
    "OK " + value1 + " " + value2
  }

  initialize()
}

