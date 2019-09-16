package cask.endpoints

import java.nio.ByteBuffer

import cask.model.Request
import cask.router.Result
import cask.util.Logger
import io.undertow.websockets.WebSocketConnectionCallback
import io.undertow.websockets.core.{AbstractReceiveListener, BufferedBinaryMessage, BufferedTextMessage, CloseMessage, WebSocketChannel, WebSockets}
import io.undertow.websockets.spi.WebSocketHttpExchange

import scala.concurrent.ExecutionContext

sealed trait WebsocketResult
object WebsocketResult{
  implicit class Response[T](value0: cask.model.Response[T])
                            (implicit f: T => cask.model.Response.Data) extends WebsocketResult{
    def value = value0.map(f)
  }
  implicit class Listener(val value: WebSocketConnectionCallback) extends WebsocketResult
}

class websocket(val path: String, override val subpath: Boolean = false)
  extends cask.router.Endpoint[WebsocketResult, Seq[String]]{
  val methods = Seq("websocket")
  type InputParser[T] = QueryParamReader[T]
  type OuterReturned = Result[WebsocketResult]
  def wrapFunction(ctx: Request, delegate: Delegate): OuterReturned = {
    delegate(WebEndpoint.buildMapFromQueryParams(ctx))
  }

  def wrapPathSegment(s: String): Seq[String] = Seq(s)
}

case class WsHandler(f: WsChannelActor => cask.util.BatchActor[WsActor.Event])
                    (implicit ec: ExecutionContext, log: Logger)
extends WebsocketResult with WebSocketConnectionCallback {
   def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
     val actor = f(new WsChannelActor(channel))
     channel.getReceiveSetter.set(
       new AbstractReceiveListener() {
         override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) = {
           actor.send(WsActor.Text(message.getData))
         }

         override def onFullBinaryMessage(channel: WebSocketChannel, message: BufferedBinaryMessage): Unit = {
           actor.send(WsActor.Binary(
             WebSockets.mergeBuffers(message.getData.getResource:_*).array()
           ))
         }

         override def onFullPingMessage(channel: WebSocketChannel, message: BufferedBinaryMessage): Unit = {
           actor.send(WsActor.Ping(
             WebSockets.mergeBuffers(message.getData.getResource:_*).array()
           ))
         }
         override def onFullPongMessage(channel: WebSocketChannel, message: BufferedBinaryMessage): Unit = {
           actor.send(WsActor.Pong(
             WebSockets.mergeBuffers(message.getData.getResource:_*).array()
           ))
         }

         override def onCloseMessage(cm: CloseMessage, channel: WebSocketChannel) = {
           actor.send(WsActor.Close(cm.getCode, cm.getReason))
         }
       }
     )
    channel.resumeReceives()
  }
}

class WsChannelActor(channel: WebSocketChannel)
                    (implicit ec: ExecutionContext, log: Logger)
extends cask.util.BatchActor[WsActor.Event]{
  def run(items: Seq[WsActor.Event]): Unit = items.foreach{
    case WsActor.Text(value) => WebSockets.sendTextBlocking(value, channel)
    case WsActor.Binary(value) => WebSockets.sendBinaryBlocking(ByteBuffer.wrap(value), channel)
    case WsActor.Ping(value) => WebSockets.sendPingBlocking(ByteBuffer.wrap(value), channel)
    case WsActor.Pong(value) => WebSockets.sendPingBlocking(ByteBuffer.wrap(value), channel)
    case WsActor.Close(code, reason) => WebSockets.sendCloseBlocking(code, reason, channel)
  }
}

case class WsActor(handle: PartialFunction[WsActor.Event, Unit])
                  (implicit ec: ExecutionContext, log: Logger)
extends cask.util.BatchActor[WsActor.Event]{
  def run(items: Seq[WsActor.Event]): Unit = {
    items.foreach(handle.applyOrElse(_, (x: WsActor.Event) => ()))
  }
}

object WsActor{
  trait Event
  case class Text(value: String) extends Event
  case class Binary(value: Array[Byte]) extends Event
  case class Ping(value: Array[Byte] = Array.empty[Byte]) extends Event
  case class Pong(value: Array[Byte] = Array.empty[Byte]) extends Event
  case class Close(code: Int = Close.NormalClosure, reason: String = "") extends Event
  object Close{
    val NormalClosure = CloseMessage.NORMAL_CLOSURE
    val GoingAway = CloseMessage.GOING_AWAY
    val WrongCode = CloseMessage.WRONG_CODE
    val ProtocolError = CloseMessage.PROTOCOL_ERROR
    val MsgContainsInvalidData = CloseMessage.MSG_CONTAINS_INVALID_DATA
    val MsgViolatesPolicy = CloseMessage.MSG_VIOLATES_POLICY
    val MsgTooBig = CloseMessage.MSG_TOO_BIG
    val MissingExtensions = CloseMessage.MISSING_EXTENSIONS
    val UnexpectedError = CloseMessage.UNEXPECTED_ERROR
  }
}
