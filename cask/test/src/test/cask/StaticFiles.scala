package test.cask

object StaticFiles extends cask.MainRoutes{
  @cask.get("/")
  def index() = {
    "Hello!"
  }

  @cask.static("/static")
  def staticRoutes = "cask/test/src/test/cask"

  initialize()
}
