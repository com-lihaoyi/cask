package cask.util

import org.scalajs.dom

abstract class WebsocketClientImpl(url: String) extends WebsocketBase{
  var websocket: dom.WebSocket = null
  var closed = false
  def connect(): Unit = {
    websocket = new dom.WebSocket(url)

    websocket.onopen = (e: dom.Event) => onOpen()
    websocket.onmessage = (e: dom.MessageEvent) => onMessage(e.data.asInstanceOf[String])
    websocket.onclose = (e: dom.CloseEvent) => {
      closed = true
      onClose(e.code, e.reason)
    }
    websocket.onerror = (e: dom.Event) => onError(new Exception(e.toString))
  }
  def onOpen(): Unit

  def send(value: String) = try {
    websocket.send(value)
    true
  } catch{case e: scala.scalajs.js.JavaScriptException => false}


  def send(value: Array[Byte]) = ???
  def onError(ex: Exception): Unit
  def onMessage(value: String): Unit
  def onClose(code: Int, reason: String): Unit
  def close(): Unit = websocket.close()
  def isClosed() = closed
}