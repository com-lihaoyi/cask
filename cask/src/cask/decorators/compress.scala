package cask.decorators
import java.io.{ByteArrayOutputStream, OutputStream}
import java.util.zip.{DeflaterOutputStream, GZIPOutputStream}

import cask.internal.Router
import cask.model.{ParamContext, Response}

import collection.JavaConverters._
class compress extends cask.Decorator{
  def wrapFunction(ctx: ParamContext, delegate: Delegate) = {
    val acceptEncodings = ctx.exchange.getRequestHeaders.get("Accept-Encoding").asScala.flatMap(_.split(", "))
    delegate(Map()) match{
      case Router.Result.Success(v) =>
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
        Router.Result.Success(
          Response(
            newData,
            v.statusCode,
            v.headers ++ newHeaders,
            v.cookies
          )
        )
      case e: Router.Result.Error => e
    }
  }
}
