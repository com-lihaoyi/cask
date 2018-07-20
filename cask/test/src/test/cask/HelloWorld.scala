package test.cask

object HelloWorld extends cask.MainRoutes{
  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  initialize()
}
