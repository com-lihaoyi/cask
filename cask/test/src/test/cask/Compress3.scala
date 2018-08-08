package test.cask

object Compress3 extends cask.Routes{

  @cask.get("/")
  def hello() = {
    "Hello World! Hello World! Hello World!"
  }

  initialize()
}

object Compress3Main extends cask.Main(Compress3){
  override def mainDecorators = Seq(new cask.decorators.compress())
}