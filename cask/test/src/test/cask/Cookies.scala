package test.cask

object Cookies extends cask.MainRoutes{
  @cask.get("/read-cookie")
  def readCookies(cookies: cask.Cookies) = {
    val username = cookies.value.get("my-username")
    username.map(_.value).toString
  }

  @cask.get("/store-cookie")
  def storeCookies() = {
    cask.Response(
      "Cookies Set!",
      cookies = Seq(cask.Cookie("my-username", "the username"))
    )
  }

  @cask.get("/delete-cookie")
  def deleteCookie() = {
    cask.Response(
      "Cookies Deleted!",
      cookies = Seq(cask.Cookie("my-username", "the username", expires = java.time.Instant.EPOCH))
    )
  }

  initialize()
}

