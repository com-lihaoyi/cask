package cask.model

import io.undertow.server.HttpServerExchange

case class ParamContext(exchange: HttpServerExchange, remaining: Seq[String])
