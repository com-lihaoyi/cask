package cask.endpoints

import cask.internal.Router
import cask.main.Routes
import cask.model.{FormValue, ParamContext, Response}
import io.undertow.server.handlers.form.FormParserFactory

import collection.JavaConverters._

sealed trait FormReader[T] extends Router.ArgReader[Seq[FormValue], T, ParamContext]
object FormReader{
  implicit def paramFormReader[T: QueryParamReader] = new FormReader[T]{
    def arity = implicitly[QueryParamReader[T]].arity

    def read(ctx: ParamContext, label: String, input: Seq[FormValue]) = {
      implicitly[QueryParamReader[T]].read(ctx, label, if (input == null) null else input.map(_.value))
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
class postForm(val path: String, override val subpath: Boolean = false) extends Routes.Endpoint[Response]{

  val methods = Seq("post")
  type InputType = Seq[FormValue]
  type InputParser[T] = FormReader[T]
  def getParamValues(ctx: ParamContext) = {
    val formData = FormParserFactory.builder().build().createParser(ctx.exchange).parseBlocking()
    val formDataBindings =
      formData
        .iterator()
        .asScala
        .map(k => (k, formData.get(k).asScala.map(FormValue.fromUndertow).toSeq))
        .toMap
    formDataBindings
  }
  def wrapPathSegment(s: String): InputType = Seq(FormValue.Plain(s, new io.undertow.util.HeaderMap))
}

