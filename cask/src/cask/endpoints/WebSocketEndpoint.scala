package cask.endpoints

import cask.internal.Router
import cask.model.Request
import io.undertow.websockets.WebSocketConnectionCallback

sealed trait WebsocketResult
object WebsocketResult{
  implicit class Response(val value: cask.model.Response) extends WebsocketResult
  implicit class Listener(val value: WebSocketConnectionCallback) extends WebsocketResult
}

class websocket(val path: String, override val subpath: Boolean = false) extends cask.main.BaseEndpoint{
  type Output = WebsocketResult
  val methods = Seq("websocket")
  type Input = Seq[String]
  type InputParser[T] = QueryParamReader[T]
  type Returned = Router.Result[WebsocketResult]
  def wrapFunction(ctx: Request, delegate: Delegate): Returned = delegate(Map())

  def wrapPathSegment(s: String): Input = Seq(s)


}
