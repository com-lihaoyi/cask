package cask.endpoints

import cask.internal.{Router, Util}
import cask.main.{Endpoint, Routes}
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
class postForm(val path: String, override val subpath: Boolean = false) extends Endpoint[Response]{

  val methods = Seq("post")
  type Input = Seq[FormValue]
  type InputParser[T] = FormReader[T]
  def getRawParams(ctx: ParamContext) = {
    for{
      formData <-
        try Right(FormParserFactory.builder().build().createParser(ctx.exchange).parseBlocking())
        catch{case e: Exception => Left(cask.model.Response(
          "Unable to parse form data: " + e + "\n" + Util.stackTraceString(e)
        ))}
    } yield {
      val formDataBindings =
        formData
          .iterator()
          .asScala
          .map(k => (k, formData.get(k).asScala.map(FormValue.fromUndertow).toSeq))
          .toMap
      formDataBindings
    }
  }
  def wrapPathSegment(s: String): Input = Seq(FormValue.Plain(s, new io.undertow.util.HeaderMap))
}

