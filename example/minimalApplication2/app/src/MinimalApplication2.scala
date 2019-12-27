package app

case class MinimalRoutes()(implicit val log: cask.Logger) extends cask.Routes{
  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  @cask.post("/do-thing")
  def doThing(request: cask.Request) = {
    request.text().reverse
  }

  initialize()
}
object MinimalRoutesMain extends cask.Main{
  val allRoutes = Seq(MinimalRoutes())
}