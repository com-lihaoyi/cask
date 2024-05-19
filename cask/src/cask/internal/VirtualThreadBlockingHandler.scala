package cask.internal

import io.undertow.server.{HttpHandler, HttpServerExchange}

private[cask] final class VirtualThreadBlockingHandler(val handler: HttpHandler)
  extends HttpHandler {
  override def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    exchange.dispatch(VirtualThreadBlockingHandler.EXECUTOR, handler)
  }
}

private[cask] object VirtualThreadBlockingHandler {
  private lazy val EXECUTOR = new NewThreadPerTaskExecutor(
    VirtualThreadSupport.create("cask"))
}
