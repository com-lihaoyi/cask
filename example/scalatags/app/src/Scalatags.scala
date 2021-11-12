package app
import scalatags.Text.all._
object Scalatags extends cask.MainRoutes{
  @cask.get("/")
  def hello() = {
    cask.Response( 
      doctype("html")(
        html(
          body(
            h1("Hello World"),
            p("I am cow")
          )
        )
      ),
      headers = Seq("Content-Type" -> "text/html") 
    )
  }

  initialize()
}
