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
    delegate(Map()).transform{ case v: cask.Response[_]  =>
      val (newData, newHeaders) = if (acceptEncodings.exists(_.toLowerCase == "gzip")) {
        new Response.Data {
          def write(out: OutputStream): Unit = {
            val wrap = new GZIPOutputStream(out)
            v.data.write(wrap)
            wrap.flush()
            wrap.close()
          }
        } -> Seq("Content-Encoding" -> "gzip")
      }else if (acceptEncodings.exists(_.toLowerCase == "deflate")){
        new Response.Data {
          def write(out: OutputStream): Unit = {
            val wrap = new DeflaterOutputStream(out)
            v.data.write(wrap)
            wrap.flush()
          }
        } -> Seq("Content-Encoding" -> "deflate")
      }else v.data -> Nil
      Response(
        newData,
        v.statusCode,
        v.headers ++ newHeaders,
        v.cookies
      )
    }

  }
}
