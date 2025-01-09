package cask.util

import java.net.{InetSocketAddress, ServerSocket}
import scala.util.control.NonFatal

object SocketUtils {
  def getFreeTcpPort: Int = synchronized {
    var serverSocket: ServerSocket = null
    try {
      serverSocket = new ServerSocket(0)
      serverSocket.getLocalPort
    } catch {
      case NonFatal(e) => throw e
    } finally {
      if (serverSocket != null)
        serverSocket.close()
    }
  }
}
