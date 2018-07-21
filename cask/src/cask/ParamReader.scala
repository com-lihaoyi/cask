package cask

import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.{FormData, FormParserFactory}

abstract class ParamReader[T]
  extends Router.ArgReader[T, (HttpServerExchange, Seq[String])]{
  def arity: Int
  def read(ctx: (HttpServerExchange, Seq[String]), v: Seq[String]): T
}
object ParamReader{
  class SimpleParam[T](f: String => T) extends ParamReader[T]{
    def arity = 1
    def read(ctx: (HttpServerExchange, Seq[String]), v: Seq[String]): T = f(v.head)
  }
  class NilParam[T](f: (HttpServerExchange, Seq[String]) => T) extends ParamReader[T]{
    def arity = 0
    def read(ctx: (HttpServerExchange, Seq[String]), v: Seq[String]): T = f(ctx._1, ctx._2)
  }
  implicit object StringParam extends SimpleParam[String](x => x)
  implicit object BooleanParam extends SimpleParam[Boolean](_.toBoolean)
  implicit object ByteParam extends SimpleParam[Byte](_.toByte)
  implicit object ShortParam extends SimpleParam[Short](_.toShort)
  implicit object IntParam extends SimpleParam[Int](_.toInt)
  implicit object LongParam extends SimpleParam[Long](_.toLong)
  implicit object DoubleParam extends SimpleParam[Double](_.toDouble)
  implicit object FloatParam extends SimpleParam[Float](_.toFloat)
  implicit def SeqParam[T: ParamReader] = new ParamReader[Seq[T]]{
    def arity = 1
    def read(ctx: (HttpServerExchange, Seq[String]), v: Seq[String]): Seq[T] = {
      v.map(x => implicitly[ParamReader[T]].read(ctx, Seq(x)))
    }
  }

  implicit object HttpExchangeParam extends NilParam[HttpServerExchange]((server, remaining) => server)
  implicit object SubpathParam extends NilParam[Subpath]((server, remaining) => new Subpath(remaining))
  implicit object CookieParam extends NilParam[Cookies]((server, remaining) => {
    import collection.JavaConverters._
    new Cookies(server.getRequestCookies.asScala.toMap.map{case (k, v) => (k, Cookie.fromUndertow(v))})
  })
  implicit object FormDataParam extends NilParam[FormData]((server, remaining) =>
    FormParserFactory.builder().build().createParser(server).parseBlocking()
  )
}

class Subpath(val value: Seq[String])
class Cookies(val value: Map[String, Cookie])