package app
object Twirl extends cask.MainRoutes{
  @cask.get("/")
  def hello() = {
    val payload = "<!doctype html>" + html.hello("Hello World")
    cask.Response(payload, 200, Seq(("Content-Type", "text/html")) )
  }

  initialize()
}
