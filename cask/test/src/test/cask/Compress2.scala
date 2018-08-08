package test.cask

object Compress2 extends cask.Routes{
  override def decorators = Seq(new cask.decorators.compress())

  @cask.get("/")
  def hello() = {
    "Hello World! Hello World! Hello World!"
  }

  initialize()
}

object Compress2Main extends cask.Main(Compress2)