package cask.endpoints

import cask.internal.Util
import cask.router.HttpEndpoint
import cask.model._
import cask.router.{ArgReader, Result}
import io.undertow.server.handlers.form.FormParserFactory

import collection.JavaConverters._

sealed trait FormReader[T] extends ArgReader[Seq[FormEntry], T, Request]
object FormReader{
  implicit def paramFormReader[T: QueryParamReader]: FormReader[T] = new FormReader[T]{
    def arity = implicitly[QueryParamReader[T]].arity

    def read(ctx: Request, label: String, input: Seq[FormEntry]) = {
      implicitly[QueryParamReader[T]].read(ctx, label, if (input == null) null else input.map(_.valueOrFileName))
    }
  }

  implicit def formEntryReader: FormReader[FormEntry] = new FormReader[FormEntry]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input.head
  }
  implicit def formEntriesReader: FormReader[Seq[FormEntry]] = new FormReader[Seq[FormEntry]]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input
  }

  implicit def formValueReader: FormReader[FormValue] = new FormReader[FormValue]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input.head.asInstanceOf[FormValue]
  }
  implicit def formValuesReader: FormReader[Seq[FormValue]] = new FormReader[Seq[FormValue]]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input.map(_.asInstanceOf[FormValue])
  }
  implicit def formFileReader: FormReader[FormFile] = new FormReader[FormFile]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input.head.asInstanceOf[FormFile]
  }
  implicit def formFilesReader: FormReader[Seq[FormFile]] = new FormReader[Seq[FormFile]]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input.map(_.asInstanceOf[FormFile])
  }
}
class postForm(val path: String, override val subpath: Boolean = false)
  extends HttpEndpoint[Response.Raw, Seq[FormEntry]] {

  val methods = Seq("post")
  type InputParser[T] = FormReader[T]
  def wrapFunction(ctx: Request,
                       delegate: Delegate): Result[Response.Raw] = {
    try {
      val formData = FormParserFactory.builder().build().createParser(ctx.exchange).parseBlocking()
      delegate(
        formData
          .iterator()
          .asScala
          .map(k => (k, formData.get(k).asScala.map(FormEntry.fromUndertow).toSeq))
          .toMap
      )
    } catch{case e: Exception =>
      Result.Success(cask.model.Response(
        "Unable to parse form data: " + e + "\n" + Util.stackTraceString(e),
        statusCode = 400
      ))
    }
  }

  def wrapPathSegment(s: String): Seq[FormEntry] = Seq(FormValue(s, new io.undertow.util.HeaderMap))
}

