package cask.util

object Ws{
  trait Event
  case class Text(value: String) extends Event
  case class Binary(value: Array[Byte]) extends Event
  case class Ping(value: Array[Byte] = Array.empty[Byte]) extends Event
  case class Pong(value: Array[Byte] = Array.empty[Byte]) extends Event
  case class Close(code: Int = Close.NormalClosure, reason: String = "") extends Event
  case class Error(e: Throwable) extends Event
  case class ChannelClosed() extends Event
  object Close{
    // Taken from io.undertow.websockets.core.CloseMessage.*
    val NormalClosure = 1000
    val GoingAway = 1001
    val WrongCode = 1002
    val ProtocolError = 1003
    val MsgContainsInvalidData = 1007
    val MsgViolatesPolicy = 1008
    val MsgTooBig = 1009
    val MissingExtensions = 1010
    val UnexpectedError = 1011
  }
}
