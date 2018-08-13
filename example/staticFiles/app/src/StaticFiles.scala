package app
object StaticFiles extends cask.MainRoutes{
  @cask.get("/")
  def index() = {
    "Hello!"
  }

  @cask.staticFiles("/static/file")
  def staticFileRoutes() = "app/resources/cask"

  @cask.staticResources("/static/resource")
  def staticResourceRoutes() = "cask"

  initialize()
}
