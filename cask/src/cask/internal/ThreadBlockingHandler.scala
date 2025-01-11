package cask.internal

import io.undertow.server.{HttpHandler, HttpServerExchange}

import java.util.concurrent.Executor

/**
 * A handler that dispatches the request to the given handler using the given executor.
 * */
final class ThreadBlockingHandler(executor: Executor, handler: HttpHandler) extends HttpHandler {
  require(executor ne null, "Executor should not be null")
  require(handler ne null, "Handler should not be null")

  def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    exchange.dispatch(executor, handler)
  }
}
