package cask.internal

import io.undertow.server.{HttpHandler, HttpServerExchange}

import java.util.concurrent.Executor

final class ThreadBlockingHandler(executor: Executor,
                                  handler: HttpHandler)
  extends HttpHandler {
  require(executor != null, "executor should not be null")

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    exchange.dispatch(executor, handler)
  }
}