package cask.util

abstract class WebsocketBase{
  def connect(): Unit
  def onOpen(): Unit
  def onMessage(message: String): Unit
  def onMessage(message: Array[Byte]): Unit
  def send(message: String): Boolean
  def send(message: Array[Byte]): Boolean
  def onClose(code: Int, reason: String): Unit
  def close(): Unit
  def isClosed(): Boolean
  def onError(ex: Exception): Unit
}