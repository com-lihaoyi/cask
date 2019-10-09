package app
import utest._
object ExampleTests extends TestSuite{
  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = io.undertow.Undertow.builder
      .addHttpListener(8080, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f("http://localhost:8080")
      finally server.stop()
    res
  }
  val tests = Tests{
    test("TodoServer") - withServer(TodoServer){ host =>
      val page = requests.get(host).text()
      assert(page.contains("What needs to be done?"))
    }
  }

}