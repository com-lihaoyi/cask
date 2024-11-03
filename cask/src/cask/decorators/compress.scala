package cask.decorators
import java.io.{ByteArrayOutputStream, OutputStream}
import java.util.zip.{DeflaterOutputStream, GZIPOutputStream}

import cask.model.{Request, Response}

import collection.JavaConverters._
class compress extends cask.RawDecorator{
  def wrapFunction(ctx: Request, delegate: Delegate) = {
    val acceptEncodings = Option(ctx.exchange.getRequestHeaders.get("Accept-Encoding"))
      .toSeq
      .flatMap(_.asScala)
      .flatMap(_.split(", "))
    val finalResult = delegate(ctx, Map()).transform{ case v: cask.Response.Raw  =>
      val (newData, newHeaders) = if (acceptEncodings.exists(_.toLowerCase == "gzip")) {
        new Response.Data {
          def write(out: OutputStream): Unit = {
            val wrap = new GZIPOutputStream(out)
            v.data.write(wrap)
            wrap.flush()
            wrap.close()
          }
          // Since we don't know the length of the gzipped data ahead of time,
          // we drop the content length header.
          override def headers = v.data.headers.filter(_._1 != "Content-Length")
        } -> Seq("Content-Encoding" -> "gzip")
      }else if (acceptEncodings.exists(_.toLowerCase == "deflate")){
        new Response.Data {
          def write(out: OutputStream): Unit = {
            val wrap = new DeflaterOutputStream(out)
            v.data.write(wrap)
            wrap.flush()
          }
          // Since we don't know the length of the compressed data ahead of
          // time, we drop the content length header.
          override def headers = v.data.headers.filter(_._1 != "Content-Length")
        } -> Seq("Content-Encoding" -> "deflate")
      }else v.data -> Nil
      Response(
        newData,
        v.statusCode,
        v.headers ++ newHeaders,
        v.cookies
      )
    }
    finalResult
  }
}
