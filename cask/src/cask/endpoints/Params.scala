package cask.endpoints

import cask.Cookie
import cask.endpoints.ParamReader.NilParam

class Subpath(val value: Seq[String])
object Subpath{
  implicit object SubpathParam extends NilParam[Subpath](ctx => new Subpath(ctx.remaining))

}
class Cookies(val value: Map[String, Cookie])
object Cookies{
  implicit object CookieParam extends NilParam[Cookies](ctx => {
    import collection.JavaConverters._
    new Cookies(ctx.exchange.getRequestCookies.asScala.toMap.map{case (k, v) => (k, Cookie.fromUndertow(v))})
  })
}
object CookieParam{
  implicit object SubpathParamParam extends NilParam[CookieParam](ctx =>
//    new CookieParam(ctx.exchange.getRequestCookies())
    ???
  )
}

case class CookieParam(cookie: Cookie)


object FormValue{
  def fromUndertow(from: io.undertow.server.handlers.form.FormData.FormValue) = {
    if (!from.isFile) Plain(from.getValue, from.getHeaders)
    else File(from.getValue, from.getFileName, from.getPath, from.getHeaders)
  }
  case class Plain(value: String,
                   headers: io.undertow.util.HeaderMap) extends FormValue

  case class File(value: String,
                  fileName: String,
                  filePath: java.nio.file.Path,
                  headers: io.undertow.util.HeaderMap) extends FormValue
}
sealed trait FormValue{
  def value: String
  def headers: io.undertow.util.HeaderMap
  def asFile: Option[FormValue.File] = this match{
    case p: FormValue.Plain => None
    case p: FormValue.File => Some(p)
  }
}
