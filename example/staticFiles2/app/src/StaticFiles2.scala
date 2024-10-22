package app
object StaticFiles2 extends cask.MainRoutes{
  @cask.get("/")
  def index() = {
    "Hello!"
  }

  @cask.staticFiles("/static/file", headers = Seq("Cache-Control" -> "max-age=31536000"))
  def staticFileRoutes() = "resources/cask"

  @cask.decorators.compress
  @cask.staticResources("/static/resource")
  def staticResourceRoutes() = "cask"

  @cask.staticResources("/static/resource2")
  def staticResourceRoutes2() = "."

  initialize()
}
