package cask.endpoints

import cask.endpoints.ParamReader.NilParam
import io.undertow.server.handlers.CookieImpl

class Subpath(val value: Seq[String])
object Subpath{
  implicit object SubpathParam extends NilParam[Subpath]((ctx, label) => new Subpath(ctx.remaining))
}


object Cookie{
  implicit object CookieParam extends NilParam[Cookie]((ctx, label) =>
    Cookie.fromUndertow(ctx.exchange.getRequestCookies().get(label))
  )
  def fromUndertow(from: io.undertow.server.handlers.Cookie): Cookie = {
    Cookie(
      from.getName,
      from.getValue,
      from.getComment,
      from.getDomain,
      if (from.getExpires == null) null else from.getExpires.toInstant,
      from.getMaxAge,
      from.getPath,
      from.getVersion,
      from.isDiscard,
      from.isHttpOnly,
      from.isSecure
    )
  }
  def toUndertow(from: Cookie): io.undertow.server.handlers.Cookie = {
    val out = new CookieImpl(from.name, from.value)
    out.setComment(from.comment)
    out.setDomain(from.domain)
    out.setExpires(if (from.expires == null) null else java.util.Date.from(from.expires))
    out.setMaxAge(from.maxAge)
    out.setPath(from.path)
    out.setVersion(from.version)
    out.setDiscard(from.discard)
    out.setHttpOnly(from.httpOnly)
    out.setSecure(from.secure)
  }
}
case class Cookie(name: String,
                  value: String,
                  comment: String = null,
                  domain: String = null,
                  expires: java.time.Instant = null,
                  maxAge: Integer = null,
                  path: String = null,
                  version: Int = 1,
                  discard: Boolean = false,
                  httpOnly: Boolean = false,
                  secure: Boolean = false) {

}


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
