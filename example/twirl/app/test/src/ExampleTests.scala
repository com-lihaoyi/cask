package app
import io.undertow.Undertow

import utest._

object ExampleTests extends TestSuite{
  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = Undertow.builder
      .addHttpListener(8080, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f("http://localhost:8080")
      finally server.stop()
    res
  }

  val tests = Tests {
    test("Twirl") - withServer(Twirl) { host =>
      val body = requests.get(host).text()

      assert(
        body.contains("<h1>Hello World</h1>"),
        body.contains("<p>I am cow</p>"),
      )
    }
  }
}
