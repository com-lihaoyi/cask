package cask.endpoints

import cask.internal.Router
import cask.model.{Cookie, Request}
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.{FormData, FormParserFactory}

abstract class ParamReader[T] extends Router.ArgReader[Unit, T, cask.model.Request]{
  def arity: Int
  def read(ctx: cask.model.Request, label: String, v: Unit): T
}
object ParamReader{
  class NilParam[T](f: (Request, String) => T) extends ParamReader[T]{
    def arity = 0
    def read(ctx: cask.model.Request, label: String, v: Unit): T = f(ctx, label)
  }
  implicit object HttpExchangeParam extends NilParam[HttpServerExchange]((ctx, label) => ctx.exchange)

  implicit object FormDataParam extends NilParam[FormData]((ctx, label) =>
    FormParserFactory.builder().build().createParser(ctx.exchange).parseBlocking()
  )

  implicit object RequestParam extends NilParam[Request]((ctx, label) => ctx)

  implicit object CookieParam extends NilParam[Cookie]((ctx, label) =>
    Cookie.fromUndertow(ctx.exchange.getRequestCookies().get(label))
  )
}
