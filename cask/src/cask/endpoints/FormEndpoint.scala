package cask.endpoints

import cask.internal.Router.EntryPoint
import cask.internal.Router
import cask.main.Routes
import cask.model.{ParamContext, Response}
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.FormParserFactory

import collection.JavaConverters._

sealed trait FormReader[T] extends Router.ArgReader[Seq[FormValue], T, ParamContext]
object FormReader{
  implicit def paramFormReader[T: QueryParamReader] = new FormReader[T]{
    def arity = implicitly[QueryParamReader[T]].arity

    def read(ctx: ParamContext, label: String, input: Seq[FormValue]) = {
      implicitly[QueryParamReader[T]].read(ctx, label, input.map(_.value))
    }
  }
  implicit def formValueReader = new FormReader[FormValue]{
    def arity = 1
    def read(ctx: ParamContext, label: String, input: Seq[FormValue]) = input.head
  }
  implicit def formValuesReader = new FormReader[Seq[FormValue]]{
    def arity = 1
    def read(ctx: ParamContext, label: String, input: Seq[FormValue]) = input
  }
  implicit def formValueFileReader = new FormReader[FormValue.File]{
    def arity = 1
    def read(ctx: ParamContext, label: String, input: Seq[FormValue]) = input.head.asFile.get
  }
  implicit def formValuesFileReader = new FormReader[Seq[FormValue.File]]{
    def arity = 1
    def read(ctx: ParamContext, label: String, input: Seq[FormValue]) = input.map(_.asFile.get)
  }
}
class postForm(val path: String, override val subpath: Boolean = false) extends Endpoint[Response]{
  type InputType = Seq[FormValue]
  def wrapMethodOutput(t: Response) = t
  def parseMethodInput[T](implicit p: FormReader[T]) = p
  def handle(ctx: ParamContext,
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Seq[FormValue], Routes, ParamContext]): Router.Result[Response] = {

    val formData = FormParserFactory.builder().build().createParser(ctx.exchange).parseBlocking()
    val formDataBindings =
      formData
        .iterator()
        .asScala
        .map(k => (k, formData.get(k).asScala.map(FormValue.fromUndertow).toSeq))


    val pathBindings =
      bindings.map{case (k, v) => (k, Seq(new FormValue.Plain(v, new io.undertow.util.HeaderMap())))}

    entryPoint.invoke(routes, ctx, pathBindings ++ formDataBindings)
      .asInstanceOf[Router.Result[Response]]
  }
}
