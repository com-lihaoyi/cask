package cask

import io.undertow.server.HttpServerExchange

class ParamReader[T](val arity: Int,
                     val read0: (HttpServerExchange, Seq[String], Seq[String]) => T)
  extends Router.ArgReader[T, (HttpServerExchange, Seq[String])]{
  def read(ctx: (HttpServerExchange, Seq[String]), v: Seq[String]): T = read0(ctx._1, v, ctx._2)
}
object ParamReader{
  implicit object StringParam extends ParamReader[String](1, (h, x, r) => x.head)
  implicit object BooleanParam extends ParamReader[Boolean](1, (h, x, r) => x.head.toBoolean)
  implicit object ByteParam extends ParamReader[Byte](1, (h, x, r) => x.head.toByte)
  implicit object ShortParam extends ParamReader[Short](1, (h, x, r) => x.head.toShort)
  implicit object IntParam extends ParamReader[Int](1, (h, x, r) => x.head.toInt)
  implicit object LongParam extends ParamReader[Long](1, (h, x, r) => x.head.toLong)
  implicit object DoubleParam extends ParamReader[Double](1, (h, x, r) => x.head.toDouble)
  implicit object FloatParam extends ParamReader[Float](1, (h, x, r) => x.head.toFloat)
  implicit def SeqParam[T: ParamReader] =
    new ParamReader[Seq[T]](1, (h, s, r) => s.map(x => implicitly[ParamReader[T]].read((h, r), Seq(x))))

  implicit object HttpExchangeParam extends ParamReader[HttpServerExchange](0, (h, x, r) => h)
  implicit object SubpathParam extends ParamReader[Subpath](0, (h, x, r) => new Subpath(r))
}

class Subpath(val value: Seq[String])