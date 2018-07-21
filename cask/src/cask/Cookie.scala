package cask

import io.undertow.server.handlers.CookieImpl

object Cookie{
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
