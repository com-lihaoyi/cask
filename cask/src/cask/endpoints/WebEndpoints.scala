package cask.endpoints

import cask.internal.Router
import cask.internal.Router.EntryPoint
import cask.main.Routes
import cask.model.{BaseResponse, ParamContext}

import collection.JavaConverters._


trait WebEndpoint extends Routes.Endpoint[BaseResponse]{
  type InputType = Seq[String]
  type InputParser[T] = QueryParamReader[T]
  def getParamValues(ctx: ParamContext) = ctx.exchange.getQueryParameters
    .asScala
    .map{case (k, vs) => (k, vs.asScala.toArray.toSeq)}
    .toMap
  def wrapPathSegment(s: String) = Seq(s)
}
class get(val path: String, override val subpath: Boolean = false) extends WebEndpoint{
  val methods = Seq("get")
}
class post(val path: String, override val subpath: Boolean = false) extends WebEndpoint{
  val methods = Seq("post")
}
class put(val path: String, override val subpath: Boolean = false) extends WebEndpoint{
  val methods = Seq("put")
}
class route(val path: String, val methods: Seq[String], override val subpath: Boolean = false) extends WebEndpoint

abstract class QueryParamReader[T]
  extends Router.ArgReader[Seq[String], T, cask.model.ParamContext]{
  def arity: Int
  def read(ctx: cask.model.ParamContext, label: String, v: Seq[String]): T
}
object QueryParamReader{
  class SimpleParam[T](f: String => T) extends QueryParamReader[T]{
    def arity = 1
    def read(ctx: cask.model.ParamContext, label: String, v: Seq[String]): T = f(v.head)
  }

  implicit object StringParam extends SimpleParam[String](x => x)
  implicit object BooleanParam extends SimpleParam[Boolean](_.toBoolean)
  implicit object ByteParam extends SimpleParam[Byte](_.toByte)
  implicit object ShortParam extends SimpleParam[Short](_.toShort)
  implicit object IntParam extends SimpleParam[Int](_.toInt)
  implicit object LongParam extends SimpleParam[Long](_.toLong)
  implicit object DoubleParam extends SimpleParam[Double](_.toDouble)
  implicit object FloatParam extends SimpleParam[Float](_.toFloat)
  implicit def SeqParam[T: QueryParamReader] = new QueryParamReader[Seq[T]]{
    def arity = 1
    def read(ctx: cask.model.ParamContext, label: String, v: Seq[String]): Seq[T] = {
      v.map(x => implicitly[QueryParamReader[T]].read(ctx, label, Seq(x)))
    }
  }
  implicit def paramReader[T: ParamReader] = new QueryParamReader[T] {
    override def arity = 0

    override def read(ctx: cask.model.ParamContext, label: String, v: Seq[String]) = {
      implicitly[ParamReader[T]].read(ctx, label, v)
    }
  }

}
