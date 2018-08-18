package cask.endpoints

import cask.internal.{Router, Util}
import cask.main.Endpoint
import cask.model._
import io.undertow.server.handlers.form.FormParserFactory

import collection.JavaConverters._

sealed trait FormReader[T] extends Router.ArgReader[Seq[FormEntry], T, Request]
object FormReader{
  implicit def paramFormReader[T: QueryParamReader] = new FormReader[T]{
    def arity = implicitly[QueryParamReader[T]].arity

    def read(ctx: Request, label: String, input: Seq[FormEntry]) = {
      implicitly[QueryParamReader[T]].read(ctx, label, if (input == null) null else input.map(_.valueOrFileName))
    }
  }

  implicit def formEntryReader = new FormReader[FormEntry]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input.head
  }
  implicit def formEntriesReader = new FormReader[Seq[FormEntry]]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input
  }

  implicit def formValueReader = new FormReader[FormValue]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input.head.asInstanceOf[FormValue]
  }
  implicit def formValuesReader = new FormReader[Seq[FormValue]]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input.map(_.asInstanceOf[FormValue])
  }
  implicit def formFileReader = new FormReader[FormFile]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input.head.asInstanceOf[FormFile]
  }
  implicit def formFilesReader = new FormReader[Seq[FormFile]]{
    def arity = 1
    def read(ctx: Request, label: String, input: Seq[FormEntry]) = input.map(_.asInstanceOf[FormFile])
  }
}
class postForm(val path: String, override val subpath: Boolean = false) extends Endpoint {
  type Output = Response

  val methods = Seq("post")
  type Input = Seq[FormEntry]
  type InputParser[T] = FormReader[T]
  def wrapFunction(ctx: Request,
                       delegate: Map[String, Input] => Router.Result[Output]): Router.Result[Response] = {
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
      Router.Result.Success(cask.model.Response(
        "Unable to parse form data: " + e + "\n" + Util.stackTraceString(e)
      ))
    }
  }

  def wrapPathSegment(s: String): Input = Seq(FormValue(s, new io.undertow.util.HeaderMap))
}

