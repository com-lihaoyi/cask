package test.cask

object StaticFiles extends cask.MainRoutes{
  @cask.get("/")
  def index() = {
    "Hello!"
  }

  @cask.static("/static")
  def staticRoutes = "cask/resources/cask"

  initialize()
}
