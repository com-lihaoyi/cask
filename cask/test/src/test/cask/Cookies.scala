package test.cask

import io.undertow.server.handlers.CookieImpl

object Cookies extends cask.MainRoutes{
  @cask.get("/read-cookies")
  def readCookies(cookies: cask.Cookies) = {
    val username = cookies.value.get("username")
    username.map(_.getValue).toString
  }

  @cask.get("store-cookies")
  def storeCookies() = {
    cask.Response(
      "Cookies Set!",
      cookies = Seq(new CookieImpl("username", "the username"))
    )
  }

  initialize()
}

