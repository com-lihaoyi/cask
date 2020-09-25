package cask.util
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake

abstract class WebsocketClientImpl(url: String) extends WebsocketBase{
  var websocket: Client = null
  var closed = false
  def connect(): Unit = {
    assert(closed == false)
    websocket = new Client()
    websocket.connect()
  }
  def onOpen(): Unit
  def onMessage(message: String): Unit
  def send(message: String) = try{
    websocket.send(message)
    true
  }catch{
    case e: org.java_websocket.exceptions.WebsocketNotConnectedException => false
  }
  def send(message: Array[Byte]) = try{
    websocket.send(message)
    true
  }catch{
    case e: org.java_websocket.exceptions.WebsocketNotConnectedException => false
  }
  def onClose(code: Int, reason: String): Unit
  def onError(ex: Exception): Unit
  def close(): Unit = {
    if (!closed) websocket.close()
  }
  def isClosed() = websocket.isClosed()
  class Client() extends WebSocketClient(new java.net.URI(url)){
    def onOpen(handshakedata: ServerHandshake) = {
      WebsocketClientImpl.this.onOpen()
    }
    def onMessage(message: String) = WebsocketClientImpl.this.onMessage(message)
    def onClose(code: Int, reason: String, remote: Boolean) = {
      closed = true
      WebsocketClientImpl.this.onClose(code, reason)
    }
    def onError(ex: Exception) = WebsocketClientImpl.this.onError(ex)

  }
}
