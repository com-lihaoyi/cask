package cask.model

import java.io.{InputStream, OutputStream, OutputStreamWriter}

import io.undertow.server.HttpServerExchange


trait BaseResponse{
  def data: BaseResponse.Data
  def statusCode: Int
  def headers: Seq[(String, String)]
  def cookies: Seq[Cookie]
}
object BaseResponse{
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
      def write(out: OutputStream) = b.transferTo(out)
    }
    implicit def JsonResponse[T: upickle.default.Writer](t: T) = new Data{
      def write(out: OutputStream) = implicitly[upickle.default.Writer[T]].write(
        new ujson.BaseRenderer(new OutputStreamWriter(out)),
        t
      )
    }
  }
}
case class Redirect(url: String)  extends BaseResponse{
  override def data = ""

  override def statusCode = 301

  override def headers = Seq("Location" -> url)

  override def cookies = Nil
}
case class Abort(code: Int) extends BaseResponse {
  override def data = ""

  override def statusCode = code

  override def headers = Nil

  override def cookies = Nil
}

case class Static(path: String) extends BaseResponse {
  val relPath = java.nio.file.Paths.get(path)
  val (data0: BaseResponse.Data, statusCode0) =
    if (java.nio.file.Files.exists(relPath) && java.nio.file.Files.isRegularFile(relPath)){
      (java.nio.file.Files.newInputStream(relPath), 200)
    }else{
      ("", 404)
    }
  override def data = data0

  override def statusCode = statusCode0

  override def headers = Nil

  override def cookies = Nil
}




case class Response(data: BaseResponse.Data,
                    statusCode: Int = 200,
                    headers: Seq[(String, String)] = Nil,
                    cookies: Seq[Cookie] = Nil) extends BaseResponse

