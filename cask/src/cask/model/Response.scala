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
case class Response[+T](
  data: T,
  statusCode: Int,
  headers: Seq[(String, String)],
  cookies: Seq[Cookie]
){
  def map[V](f: T => V) = new Response(f(data), statusCode, headers, cookies)
}

object Response {
  type Raw = Response[Data]
  def apply[T](data: T,
               statusCode: Int = 200,
               headers: Seq[(String, String)] = Nil,
               cookies: Seq[Cookie] = Nil) = new Response(data, statusCode, headers, cookies)
  trait Data{
    def write(out: OutputStream): Unit
  }
  trait DataCompanion[V]{
    // Put the implicit constructors for Response[Data] into the `Data` companion
    // object and all subclasses of `Data`, because for some reason putting them in
    // the `Response` companion object doesn't work properly. For the same unknown
    // reasons, we cannot have `dataResponse` and `dataResponse2` take two type
    // params T and V, and instead have to embed the implicit target type as a
    // parameter of the enclosing trait

    implicit def dataResponse[T](t: T)(implicit c: T => V): Response[V] = {
      Response(c(t))
    }

    implicit def dataResponse2[T](t: Response[T])(implicit c: T => V): Response[V] = {
      t.map(c(_))
    }
  }
  object Data extends DataCompanion[Data]{
    implicit class UnitData(s: Unit) extends Data{
      def write(out: OutputStream) = ()
    }
    implicit class WritableData[T](s: T)(implicit f: T => geny.Writable) extends Data{
      def write(out: OutputStream) = f(s).writeBytesTo(out)
    }
    implicit class NumericData[T: Numeric](s: T) extends Data{
      def write(out: OutputStream) = out.write(s.toString.getBytes)
    }
    implicit class BooleanData(s: Boolean) extends Data{
      def write(out: OutputStream) = out.write(s.toString.getBytes)
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
  def apply(path: String, headers: Seq[(String, String)]) = {
    val relPath = java.nio.file.Paths.get(path)
    val (data0, statusCode0) =
      if (java.nio.file.Files.exists(relPath) && java.nio.file.Files.isRegularFile(relPath)){
        (java.nio.file.Files.newInputStream(relPath): Response.Data, 200)
      }else{
        ("": Response.Data, 404)
      }
    Response(data0, statusCode0, headers, Nil)
  }
}
object StaticResource{
  def apply(path: String, resourceRoot: ClassLoader, headers: Seq[(String, String)]) = {
    val (data0, statusCode0) = resourceRoot.getResourceAsStream(path) match{
      case null => ("": Response.Data, 404)
      case res => (res: Response.Data, 200)
    }
    Response(data0, statusCode0, headers, Nil)
  }
}


