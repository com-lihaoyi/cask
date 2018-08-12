package app
object Compress extends cask.MainRoutes{

  @cask.decorators.compress
  @cask.get("/")
  def hello() = {
    "Hello World! Hello World! Hello World!"
  }

  initialize()
}
