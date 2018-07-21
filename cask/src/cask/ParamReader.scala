package cask

import io.undertow.server.HttpServerExchange

class ParamReader[T](val arity: Int,
                     val read0: (HttpServerExchange, Seq[String]) => T)
  extends Router.ArgReader[T, HttpServerExchange]{
  def read(ctx: HttpServerExchange, v: Seq[String]): T = read0(ctx, v)
}
object ParamReader{
  implicit object StringParam extends ParamReader[String](1, (h, x) => x.head)
  implicit object BooleanParam extends ParamReader[Boolean](1, (h, x) => x.head.toBoolean)
  implicit object ByteParam extends ParamReader[Byte](1, (h, x) => x.head.toByte)
  implicit object ShortParam extends ParamReader[Short](1, (h, x) => x.head.toShort)
  implicit object IntParam extends ParamReader[Int](1, (h, x) => x.head.toInt)
  implicit object LongParam extends ParamReader[Long](1, (h, x) => x.head.toLong)
  implicit object DoubleParam extends ParamReader[Double](1, (h, x) => x.head.toDouble)
  implicit object FloatParam extends ParamReader[Float](1, (h, x) => x.head.toFloat)
  implicit def SeqParam[T: ParamReader] =
    new ParamReader[Seq[T]](1, (h, s) => s.map(x => implicitly[ParamReader[T]].read(h, Seq(x))))

  implicit object HttpExchangeParam extends ParamReader[HttpServerExchange](0, (h, x) => h)
}