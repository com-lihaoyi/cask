package app
import utest._
object TodoTests extends TestSuite{
  def test[T](example: cask.main.BaseMain)(f: String => T): T = {
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
    'TodoServer - test(TodoServer){ host =>
      val page = requests.get(host).text()
      assert(page.contains("What needs to be done?"))
    }
  }

}