package cask

import java.net.HttpCookie

import io.undertow.server.handlers.{Cookie, CookieImpl}

object Util {
  def splitPath(p: String) =
    p.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse.split('/').filter(_.nonEmpty)
}
