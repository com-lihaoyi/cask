package cask.endpoints

import cask.Cookie
import cask.internal.Router
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.{FormData, FormParserFactory}

abstract class ParamReader[T]
  extends Router.ArgReader[Seq[String], T, cask.model.ParamContext]{
  def arity: Int
  def read(ctx: cask.model.ParamContext, v: Seq[String]): T
}
object ParamReader{
  class NilParam[T](f: cask.model.ParamContext => T) extends ParamReader[T]{
    def arity = 0
    def read(ctx: cask.model.ParamContext, v: Seq[String]): T = f(ctx)
  }
  implicit object HttpExchangeParam extends NilParam[HttpServerExchange](ctx => ctx.exchange)

  implicit object FormDataParam extends NilParam[FormData](ctx =>
    FormParserFactory.builder().build().createParser(ctx.exchange).parseBlocking()
  )
}
