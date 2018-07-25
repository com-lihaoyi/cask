package cask.endpoints

import cask.internal.Router
import cask.internal.Router.EntryPoint
import cask.main.Routes
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
class postJson(val path: String, override val subpath: Boolean = false) extends Routes.Endpoint[Response]{
  val methods = Seq("post")
  type InputType = ujson.Js.Value
  def parseMethodInput[T](implicit p: JsReader[T]) = p
  def handle(ctx: ParamContext) =
    ujson.read(new String(ctx.exchange.getInputStream.readAllBytes())).obj.toMap
  def wrapPathSegment(s: String): InputType = ujson.Js.Str(s)
}
