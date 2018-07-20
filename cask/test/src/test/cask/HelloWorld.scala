package test.cask

object HelloRoutes extends cask.Routes{
  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  initialize()
}

object HelloWorld extends cask.Main(HelloRoutes)
