package cask.endpoints

import cask.Cookie
import cask.internal.Router
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.{FormData, FormParserFactory}

abstract class ParamReader[T]
  extends Router.ArgReader[Seq[String], T, (HttpServerExchange, Seq[String])]{
  def arity: Int
  def read(ctx: (HttpServerExchange, Seq[String]), v: Seq[String]): T
}
object ParamReader{
  class NilParam[T](f: (HttpServerExchange, Seq[String]) => T) extends ParamReader[T]{
    def arity = 0
    def read(ctx: (HttpServerExchange, Seq[String]), v: Seq[String]): T = f(ctx._1, ctx._2)
  }
  implicit object HttpExchangeParam extends NilParam[HttpServerExchange]((server, remaining) => server)
  implicit object SubpathParam extends NilParam[cask.model.Subpath]((server, remaining) => new cask.model.Subpath(remaining))
  implicit object CookieParam extends NilParam[cask.model.Cookies]((server, remaining) => {
    import collection.JavaConverters._
    new cask.model.Cookies(server.getRequestCookies.asScala.toMap.map{case (k, v) => (k, Cookie.fromUndertow(v))})
  })
  implicit object FormDataParam extends NilParam[FormData]((server, remaining) =>
    FormParserFactory.builder().build().createParser(server).parseBlocking()
  )
}
