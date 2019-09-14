package cask.endpoints

import cask.internal.Router
import cask.model.Request
import io.undertow.websockets.WebSocketConnectionCallback
import collection.JavaConverters._
sealed trait WebsocketResult
object WebsocketResult{
  implicit class Response[T](value0: cask.model.Response[T])
                            (implicit f: T => cask.model.Response.Data) extends WebsocketResult{
    def value = value0.map(f)
  }
  implicit class Listener(val value: WebSocketConnectionCallback) extends WebsocketResult
}

class websocket(val path: String, override val subpath: Boolean = false)
  extends cask.main.BaseEndpoint[WebsocketResult]{
  val methods = Seq("websocket")
  type Input = Seq[String]
  type InputParser[T] = QueryParamReader[T]
  type OuterReturned = Router.Result[WebsocketResult]
  def wrapFunction(ctx: Request, delegate: Delegate): OuterReturned = {
    delegate(WebEndpoint.buildMapFromQueryParams(ctx))
  }

  def wrapPathSegment(s: String): Input = Seq(s)
}
