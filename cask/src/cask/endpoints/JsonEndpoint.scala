package cask.endpoints

import java.io.{ByteArrayOutputStream, InputStream, OutputStream, OutputStreamWriter}

import cask.internal.Util
import cask.router.HttpEndpoint
import cask.model.Response.DataCompanion
import cask.model.{Request, Response}
import cask.router.{ArgReader, Result}

import collection.JavaConverters._

sealed trait JsReader[T] extends ArgReader[ujson.Value, T, cask.model.Request]
object JsReader{
  implicit def defaultJsReader[T: upickle.default.Reader]: JsReader[T] = new JsReader[T]{
    def arity = 1

    def read(ctx: cask.model.Request, label: String, input: ujson.Value): T = {
      val reader = implicitly[upickle.default.Reader[T]]
      upickle.default.read[T](input)(reader)
    }
  }

  implicit def paramReader[T: ParamReader]: JsReader[T] = new JsReader[T] {
    override def arity = 0

    override def unknownQueryParams: Boolean = implicitly[ParamReader[T]].unknownQueryParams
    override def remainingPathSegments: Boolean = implicitly[ParamReader[T]].remainingPathSegments
    override def read(ctx: cask.model.Request, label: String, v: ujson.Value) = {
      implicitly[ParamReader[T]].read(ctx, label, Nil)
    }
  }
}
trait JsonData extends Response.Data
object JsonData extends DataCompanion[JsonData]{
  implicit class JsonDataImpl[T: upickle.default.Writer](t: T) extends JsonData{
    def headers = Seq("Content-Type" -> "application/json")
    def write(out: OutputStream) = {
      upickle.default.stream(t).writeBytesTo(out)
      out.flush()
    }
  }
}

class postJson(val path: String, override val subpath: Boolean = false)
  extends HttpEndpoint[Response[JsonData], ujson.Value]{
  val methods = Seq("post")
  type InputParser[T] = JsReader[T]

  def wrapFunction(ctx: Request, delegate: Delegate): Result[Response.Raw] = {
    val obj = for{
      json <-
        try Right(ujson.read(ctx.exchange.getInputStream))
        catch{case e: Throwable => Left(cask.model.Response(
          "Input text is invalid JSON: " + e + "\n" + Util.stackTraceString(e),
          statusCode = 400
        ))}
      obj <-
        try Right(json.obj)
        catch {case e: Throwable => Left(cask.model.Response(
          "Input JSON must be a dictionary",
          statusCode = 400
        ))}
    } yield obj.toMap
    obj match{
      case Left(r) => Result.Success(r.map(Response.Data.WritableData(_)))
      case Right(params) => delegate(params)
    }
  }
  def wrapPathSegment(s: String): ujson.Value = ujson.Str(s)
}

class getJson(val path: String, override val subpath: Boolean = false)
  extends HttpEndpoint[Response[JsonData], Seq[String]]{
  val methods = Seq("get")
  type InputParser[T] = QueryParamReader[T]

  def wrapFunction(ctx: Request, delegate: Delegate): Result[Response.Raw] = {
    delegate(WebEndpoint.buildMapFromQueryParams(ctx))
  }
  def wrapPathSegment(s: String) = Seq(s)
}
