package cask.endpoints

import cask.internal.Router
import cask.internal.Router.EntryPoint
import cask.main.Routes
import cask.model.BaseResponse
import io.undertow.server.HttpServerExchange

trait Endpoint[R]{
  type InputType
  val path: String
  def subpath: Boolean = false
  def wrapMethodOutput(t: R): Any
  def handle(exchange: HttpServerExchange,
             remaining: Seq[String],
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[InputType, Routes, (HttpServerExchange, Seq[String])]): Router.Result[BaseResponse]
}