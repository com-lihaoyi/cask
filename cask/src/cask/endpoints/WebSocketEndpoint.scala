package cask.endpoints

import cask.internal.Router
import cask.model.{ParamContext, Subpath}
import io.undertow.server.HttpServerExchange
import io.undertow.websockets.WebSocketConnectionCallback
trait WebsocketParam[T] extends Router.ArgReader[Seq[String], T, cask.model.ParamContext]

object WebsocketParam{
  class NilParam[T](f: (ParamContext, String) => T) extends WebsocketParam[T]{
    def arity = 0
    def read(ctx: ParamContext, label: String, v: Seq[String]): T = f(ctx, label)
  }
  implicit object HttpExchangeParam extends NilParam[HttpServerExchange](
    (ctx, label) => ctx.exchange
  )
  implicit object SubpathParam extends NilParam[Subpath](
    (ctx, label) => new Subpath(ctx.remaining)
  )
  class SimpleParam[T](f: String => T) extends WebsocketParam[T]{
    def arity = 1
    def read(ctx: cask.model.ParamContext, label: String, v: Seq[String]): T = f(v.head)
  }

  implicit object StringParam extends SimpleParam[String](x => x)
  implicit object BooleanParam extends SimpleParam[Boolean](_.toBoolean)
  implicit object ByteParam extends SimpleParam[Byte](_.toByte)
  implicit object ShortParam extends SimpleParam[Short](_.toShort)
  implicit object IntParam extends SimpleParam[Int](_.toInt)
  implicit object LongParam extends SimpleParam[Long](_.toLong)
  implicit object DoubleParam extends SimpleParam[Double](_.toDouble)
  implicit object FloatParam extends SimpleParam[Float](_.toFloat)
}

sealed trait WebsocketResult
object WebsocketResult{
  implicit class Response(val value: cask.model.Response) extends WebsocketResult
  implicit class Listener(val value: WebSocketConnectionCallback) extends WebsocketResult
}

class websocket(val path: String, override val subpath: Boolean = false) extends cask.main.BaseEndpoint{
  type Output = WebsocketResult
  val methods = Seq("websocket")
  type Input = Seq[String]
  type InputParser[T] = WebsocketParam[T]
  type Returned = Router.Result[WebsocketResult]
  def wrapFunction(ctx: ParamContext, delegate: Delegate): Returned = delegate(Map())

  def wrapPathSegment(s: String): Input = Seq(s)


}
