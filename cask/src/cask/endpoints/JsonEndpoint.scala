package cask.endpoints

import java.io.ByteArrayOutputStream

import cask.internal.{Router, Util}
import cask.main.Endpoint
import cask.model.{Request, Response}


sealed trait JsReader[T] extends Router.ArgReader[ujson.Value, T, cask.model.Request]
object JsReader{
  implicit def defaultJsReader[T: upickle.default.Reader] = new JsReader[T]{
    def arity = 1

    def read(ctx: cask.model.Request, label: String, input: ujson.Value): T = {
      val reader = implicitly[upickle.default.Reader[T]]
      upickle.default.read[T](input)(reader)
    }
  }

  implicit def paramReader[T: ParamReader] = new JsReader[T] {
    override def arity = 0

    override def read(ctx: cask.model.Request, label: String, v: ujson.Value) = {
      implicitly[ParamReader[T]].read(ctx, label, Nil)
    }
  }
}
class postJson(val path: String, override val subpath: Boolean = false) extends Endpoint{
  type Output = Response
  val methods = Seq("post")
  type Input = ujson.Js.Value
  type InputParser[T] = JsReader[T]
  def wrapFunction(ctx: Request,
                       delegate: Map[String, Input] => Router.Result[Output]): Router.Result[Response] = {
    val obj = for{
      str <-
        try {
          val boas = new ByteArrayOutputStream()
          Util.transferTo(ctx.exchange.getInputStream, boas)
          Right(new String(boas.toByteArray))
        }
        catch{case e: Throwable => Left(cask.model.Response(
          "Unable to deserialize input JSON text: " + e + "\n" + Util.stackTraceString(e)
        ))}
      json <-
        try Right(ujson.read(str))
        catch{case e: Throwable => Left(cask.model.Response(
          "Input text is invalid JSON: " + e + "\n" + Util.stackTraceString(e)
        ))}
      obj <-
        try Right(json.obj)
        catch {case e: Throwable => Left(cask.model.Response("Input JSON must be a dictionary"))}
    } yield obj.toMap
    obj match{
      case Left(r) => Router.Result.Success(r)
      case Right(params) => delegate(params)
    }
  }
  def wrapPathSegment(s: String): Input = ujson.Js.Str(s)
}
