package app
object Twirl extends cask.MainRoutes{
  @cask.get("/")
  def hello() = {
    "<!doctype html>" + html.hello("Hello World")
  }

  initialize()
}
