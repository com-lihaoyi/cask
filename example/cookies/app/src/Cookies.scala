package app
object Cookies extends cask.MainRoutes{
  @cask.get("/read-cookie")
  def readCookies(username: cask.Cookie) = {
    username.value
  }

  @cask.get("/store-cookie")
  def storeCookies() = {
    cask.Response(
      "Cookies Set!",
      cookies = Seq(cask.Cookie("username", "the_username"))
    )
  }

  @cask.get("/delete-cookie")
  def deleteCookie() = {
    cask.Response(
      "Cookies Deleted!",
      cookies = Seq(cask.Cookie("username", "", expires = java.time.Instant.EPOCH))
    )
  }

  initialize()
}
