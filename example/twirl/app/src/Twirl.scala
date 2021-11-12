package app
object Twirl extends cask.MainRoutes{
  @cask.get("/")
  def hello() = {
    cask.Response( 
      "<!doctype html>" + html.hello("Hello World"),
      headers = Seq("Content-Type" -> "text/html") 
    )
  }

  initialize()
}
