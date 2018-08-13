package app

import io.undertow.websockets.WebSocketConnectionCallback
import io.undertow.websockets.core.{AbstractReceiveListener, BufferedTextMessage, WebSocketChannel, WebSockets}
import io.undertow.websockets.spi.WebSocketHttpExchange

object Websockets extends cask.MainRoutes{
  @cask.websocket("/connect/:userName")
  def showUserProfile(userName: String): cask.WebsocketResult = {
    if (userName != "haoyi") cask.Response("", statusCode = 403)
    else new WebSocketConnectionCallback() {
      override def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
        channel.getReceiveSetter.set(
          new AbstractReceiveListener() {
            override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) = {
              message.getData match{
                case "" => channel.close()
                case data => WebSockets.sendTextBlocking(userName + " " + data, channel)
              }
            }
          }
        )
        channel.resumeReceives()
      }
    }
  }

  initialize()
}
