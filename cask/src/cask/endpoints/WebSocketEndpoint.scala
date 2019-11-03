package cask.endpoints

import java.nio.ByteBuffer

import cask.model.Request
import cask.router.Result
import cask.util.{Logger, Ws}
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
  extends cask.router.Endpoint[WebsocketResult, WebsocketResult, Seq[String]]{
  val methods = Seq("websocket")
  type InputParser[T] = QueryParamReader[T]
  type OuterReturned = Result[WebsocketResult]
  def wrapFunction(ctx: Request, delegate: Delegate) = {
    delegate(WebEndpoint.buildMapFromQueryParams(ctx))
  }

  def wrapPathSegment(s: String): Seq[String] = Seq(s)
}

case class WsHandler(f: WsChannelActor => cask.actor.Actor[Ws.Event])
                    (implicit ac: cask.actor.Context, log: Logger)
extends WebsocketResult with WebSocketConnectionCallback {
   def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
     channel.suspendReceives()
     val actor = f(new WsChannelActor(channel))
     // Somehow browsers closing tabs and Java processes being killed appear
     // as different events here; the former goes to AbstractReceiveListener#onClose,
     // while the latter to ChannelListener#handleEvent. Make sure we handle both cases.
     channel.addCloseTask(channel => actor.send(Ws.ChannelClosed()))
     channel.getReceiveSetter.set(
       new AbstractReceiveListener() {
         override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) = {
           actor.send(Ws.Text(message.getData))
         }

         override def onFullBinaryMessage(channel: WebSocketChannel, message: BufferedBinaryMessage): Unit = {
           actor.send(Ws.Binary(
             WebSockets.mergeBuffers(message.getData.getResource:_*).array()
           ))
         }

         override def onFullPingMessage(channel: WebSocketChannel, message: BufferedBinaryMessage): Unit = {
           actor.send(Ws.Ping(
             WebSockets.mergeBuffers(message.getData.getResource:_*).array()
           ))
         }
         override def onFullPongMessage(channel: WebSocketChannel, message: BufferedBinaryMessage): Unit = {
           actor.send(Ws.Pong(
             WebSockets.mergeBuffers(message.getData.getResource:_*).array()
           ))
         }

         override def onCloseMessage(cm: CloseMessage, channel: WebSocketChannel) = {
           actor.send(Ws.Close(cm.getCode, cm.getReason))
         }
       }
     )
    channel.resumeReceives()
  }
}

class WsChannelActor(channel: WebSocketChannel)
                    (implicit ac: cask.actor.Context, log: Logger)
extends cask.actor.SimpleActor[Ws.Event]{
  def run(item: Ws.Event): Unit = item match{
    case Ws.Text(value) => WebSockets.sendTextBlocking(value, channel)
    case Ws.Binary(value) => WebSockets.sendBinaryBlocking(ByteBuffer.wrap(value), channel)
    case Ws.Ping(value) => WebSockets.sendPingBlocking(ByteBuffer.wrap(value), channel)
    case Ws.Pong(value) => WebSockets.sendPingBlocking(ByteBuffer.wrap(value), channel)
    case Ws.Close(code, reason) => WebSockets.sendCloseBlocking(code, reason, channel)
  }
}

case class WsActor(handle: PartialFunction[Ws.Event, Unit])
                  (implicit ac: cask.actor.Context, log: Logger)
extends cask.actor.SimpleActor[Ws.Event]{
  def run(item: Ws.Event): Unit = {
    handle.lift(item)
  }
}

