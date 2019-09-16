package app

case class Compress3()(implicit val log: cask.Logger) extends cask.Routes{

  @cask.get("/")
  def hello() = {
    "Hello World! Hello World! Hello World!"
  }

  initialize()
}

object Compress3Main extends cask.Main{
  override def mainDecorators = Seq(new cask.decorators.compress())
  val allRoutes = Seq(Compress3())
}