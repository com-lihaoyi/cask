package app
import utest._
object ExampleTests extends TestSuite{
  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = io.undertow.Undertow.builder
      .addHttpListener(8081, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f("http://localhost:8081")
      finally server.stop()
    res
  }
  val tests = Tests{
    test("TodoServer") - withServer(TodoServer){ host =>
      val page = requests.get(host).text()
      assert(page.contains("What needs to be done?"))

      val cssResponse = requests.get(host + "/static/index.css")
      assert(cssResponse.text().contains("font: 14px 'Helvetica Neue', Helvetica, Arial, sans-serif;"))
      assert(cssResponse.headers("content-type") == List("text/css"))

      val jsResponse = requests.get(host + "/static/app.js")
      assert(jsResponse.text().contains("initListeners()"))
      assert(jsResponse.headers("content-type") == List("text/javascript"))
    }
  }

}