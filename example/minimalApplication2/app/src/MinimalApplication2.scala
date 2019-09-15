package app

import cask.util.Logger

case class MinimalRoutes()(implicit val log: Logger) extends cask.Routes{
  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  @cask.post("/do-thing")
  def doThing(request: cask.Request) = {
    new String(request.readAllBytes()).reverse
  }

  initialize()
}
object MinimalRoutesMain extends cask.Main{
  val allRoutes = Seq(MinimalRoutes())
}