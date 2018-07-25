package cask.endpoints

import cask.internal.Router
import cask.internal.Router.EntryPoint
import cask.main.Routes
import cask.model.{BaseResponse, ParamContext}
import io.undertow.server.HttpServerExchange

trait Endpoint[R]{
  type InputType
  val path: String
  def subpath: Boolean = false
  def wrapMethodOutput(t: R): Any
  def handle(ctx: ParamContext,
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[InputType, Routes, cask.model.ParamContext]): Router.Result[BaseResponse]
}