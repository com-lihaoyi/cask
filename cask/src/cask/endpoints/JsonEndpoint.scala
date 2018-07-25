package cask.endpoints

import cask.internal.Router
import cask.internal.Router.EntryPoint
import cask.main.Routes
import cask.model.Response
import io.undertow.server.HttpServerExchange


sealed trait JsReader[T] extends Router.ArgReader[ujson.Js.Value, T, (HttpServerExchange, Seq[String])]
object JsReader{
  implicit def defaultJsReader[T: upickle.default.Reader] = new JsReader[T]{
    def arity = 1

    def read(ctx: (HttpServerExchange, Seq[String]), input: ujson.Js.Value): T = {
      implicitly[upickle.default.Reader[T]].apply(input)
    }
  }

  implicit def paramReader[T: ParamReader] = new JsReader[T] {
    override def arity = 0

    override def read(ctx: (HttpServerExchange, Seq[String]), v: ujson.Js.Value) = {
      implicitly[ParamReader[T]].read(ctx, Nil)
    }
  }
}
class postJson(val path: String, override val subpath: Boolean = false)  extends Endpoint[Response]{
  type InputType = ujson.Js.Value
  def wrapMethodOutput(t: Response) = t
  def parseMethodInput[T](implicit p: JsReader[T]) = p
  def handle(exchange: HttpServerExchange,
             remaining: Seq[String],
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[ujson.Js.Value, Routes, (HttpServerExchange, Seq[String])]): Router.Result[Response] = {

    val js = ujson.read(new String(exchange.getInputStream.readAllBytes())).asInstanceOf[ujson.Js.Obj]

    js.obj
    val allBindings = bindings.mapValues(ujson.Js.Str(_))

    entryPoint.invoke(routes, (exchange, remaining), js.obj.toMap ++ allBindings)
      .asInstanceOf[Router.Result[Response]]
  }
}
