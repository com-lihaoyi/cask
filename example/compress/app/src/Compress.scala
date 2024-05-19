package app
object Compress extends cask.MainRoutes{

  @cask.decorators.compress
  @cask.get("/")
  def hello(): String = {
    Thread.sleep(1000) // Simulate a slow endpoint
    "Hello World! Hello World! Hello World!"
  }

  initialize()
}
