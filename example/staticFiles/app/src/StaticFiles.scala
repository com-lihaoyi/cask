package app
object StaticFiles extends cask.MainRoutes{
  @cask.get("/")
  def index() = {
    "Hello!"
  }

  @cask.staticFiles("/static/file")
  def staticFileRoutes() = "resources/cask"

  @cask.staticResources("/static/resource")
  def staticResourceRoutes() = "cask"

  @cask.staticResources("/static/resource2")
  def staticResourceRoutes2() = "."

  initialize()
}
