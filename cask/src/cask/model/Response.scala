package cask.model

import java.io.{InputStream, OutputStream, OutputStreamWriter}

import cask.internal.Util

/**
  * The basic response returned by a HTTP endpoint.
  *
  * Note that [[data]] by default can take in a wide range of types: strings,
  * bytes, uPickle JSON-convertable types or arbitrary input streams. You can
  * also construct your own implementations of `Response.Data`.
  */
case class Response(
  data: Response.Data,
  statusCode: Int,
  headers: Seq[(String, String)],
  cookies: Seq[Cookie]
)
object Response{
  def apply(data: Data,
            statusCode: Int = 200,
            headers: Seq[(String, String)] = Nil,
            cookies: Seq[Cookie] = Nil) = new Response(data, statusCode, headers, cookies)

  implicit def dataResponse[T](t: T)(implicit c: T => Data) = Response(t)
  trait Data{
    def write(out: OutputStream): Unit
  }
  object Data{
    implicit class StringData(s: String) extends Data{
      def write(out: OutputStream) = out.write(s.getBytes)
    }
    implicit class BytesData(b: Array[Byte]) extends Data{
      def write(out: OutputStream) = out.write(b)
    }
    implicit class StreamData(b: InputStream) extends Data{
      def write(out: OutputStream) = Util.transferTo(b, out)
    }
    implicit def JsonResponse[T: upickle.default.Writer](t: T) = new Data{
      def write(out: OutputStream) = implicitly[upickle.default.Writer[T]].write(
        new ujson.BaseRenderer(new OutputStreamWriter(out)),
        t
      )
    }
  }
}
object Redirect{
  def apply(url: String) = Response("", 301, Seq("Location" -> url), Nil)
}
object Abort{
  def apply(code: Int) = Response("", code, Nil, Nil)
}
object StaticFile{
  def apply(path: String) = {
    val relPath = java.nio.file.Paths.get(path)
    val (data0, statusCode0) =
      if (java.nio.file.Files.exists(relPath) && java.nio.file.Files.isRegularFile(relPath)){
        (java.nio.file.Files.newInputStream(relPath): Response.Data, 200)
      }else{
        ("": Response.Data, 404)
      }
    Response(data0, statusCode0, Nil, Nil)
  }
}
object StaticResource{
  def apply(path: String, resourceRoot: ClassLoader) = {
    val relPath = java.nio.file.Paths.get(path)
    val (data0, statusCode0) = resourceRoot.getResourceAsStream(path) match{
      case null => ("": Response.Data, 404)
      case res => (res: Response.Data, 200)
    }
    Response(data0, statusCode0, Nil, Nil)
  }
}


