package app

object MinimalRoutes extends cask.Routes{
  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  @cask.post("/do-thing")
  def doThing(request: cask.Request) = {
    new String(request.data.readAllBytes()).reverse
  }

  initialize()
}
object MinimalMain extends cask.Main(MinimalRoutes)