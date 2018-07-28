package cask.endpoints

import cask.internal.{Router, Util}
import cask.internal.Router.EntryPoint
import cask.main.{Endpoint, Routes}
import cask.model.{ParamContext, Response}


sealed trait JsReader[T] extends Router.ArgReader[ujson.Js.Value, T, cask.model.ParamContext]
object JsReader{
  implicit def defaultJsReader[T: upickle.default.Reader] = new JsReader[T]{
    def arity = 1

    def read(ctx: cask.model.ParamContext, label: String, input: ujson.Js.Value): T = {
      implicitly[upickle.default.Reader[T]].apply(input)
    }
  }

  implicit def paramReader[T: ParamReader] = new JsReader[T] {
    override def arity = 0

    override def read(ctx: cask.model.ParamContext, label: String, v: ujson.Js.Value) = {
      implicitly[ParamReader[T]].read(ctx, label, Nil)
    }
  }
}
class postJson(val path: String, override val subpath: Boolean = false) extends Endpoint[Response]{
  val methods = Seq("post")
  type Input = ujson.Js.Value
  type InputParser[T] = JsReader[T]
  def getRawParams(ctx: ParamContext) = {
    for{
      str <-
        try Right(new String(ctx.exchange.getInputStream.readAllBytes()))
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
    } yield cask.main.Decor(obj.toMap)
  }
  def wrapPathSegment(s: String): Input = ujson.Js.Str(s)
}
