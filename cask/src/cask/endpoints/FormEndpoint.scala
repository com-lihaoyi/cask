package cask.endpoints

import cask.internal.{Router, Util}
import cask.main.{Endpoint, Routes}
import cask.model._
import io.undertow.server.handlers.form.FormParserFactory

import collection.JavaConverters._

sealed trait FormReader[T] extends Router.ArgReader[Seq[FormEntry], T, ParamContext]
object FormReader{
  implicit def paramFormReader[T: QueryParamReader] = new FormReader[T]{
    def arity = implicitly[QueryParamReader[T]].arity

    def read(ctx: ParamContext, label: String, input: Seq[FormEntry]) = {
      implicitly[QueryParamReader[T]].read(ctx, label, if (input == null) null else input.map(_.valueOrFileName))
    }
  }

  implicit def formEntryReader = new FormReader[FormEntry]{
    def arity = 1
    def read(ctx: ParamContext, label: String, input: Seq[FormEntry]) = input.head
  }
  implicit def formEntriesReader = new FormReader[Seq[FormEntry]]{
    def arity = 1
    def read(ctx: ParamContext, label: String, input: Seq[FormEntry]) = input
  }

  implicit def formValueReader = new FormReader[FormValue]{
    def arity = 1
    def read(ctx: ParamContext, label: String, input: Seq[FormEntry]) = input.head.asInstanceOf[FormValue]
  }
  implicit def formValuesReader = new FormReader[Seq[FormValue]]{
    def arity = 1
    def read(ctx: ParamContext, label: String, input: Seq[FormEntry]) = input.map(_.asInstanceOf[FormValue])
  }
  implicit def formFileReader = new FormReader[FormFile]{
    def arity = 1
    def read(ctx: ParamContext, label: String, input: Seq[FormEntry]) = input.head.asInstanceOf[FormFile]
  }
  implicit def formFilesReader = new FormReader[Seq[FormFile]]{
    def arity = 1
    def read(ctx: ParamContext, label: String, input: Seq[FormEntry]) = input.map(_.asInstanceOf[FormFile])
  }
}
class postForm(val path: String, override val subpath: Boolean = false) extends Endpoint[Response]{

  val methods = Seq("post")
  type Input = Seq[FormEntry]
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
          .map(k => (k, formData.get(k).asScala.map(FormEntry.fromUndertow).toSeq))
          .toMap
      formDataBindings
    }
  }
  def wrapPathSegment(s: String): Input = Seq(FormValue(s, new io.undertow.util.HeaderMap))
}

