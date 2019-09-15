package app

import cask.util.Logger

case class Compress2()(implicit val log: Logger) extends cask.Routes{
  override def decorators = Seq(new cask.decorators.compress())

  @cask.get("/")
  def hello() = {
    "Hello World! Hello World! Hello World!"
  }

  initialize()
}

object Compress2Main extends cask.Main{
  val allRoutes = Seq(Compress2())
}
