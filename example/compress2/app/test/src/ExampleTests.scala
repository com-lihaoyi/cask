package app
import io.undertow.Undertow

import utest._

object ExampleTests extends TestSuite{
  def withServer[T](example: cask.main.BaseMain)(f: String => T): T = {
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

  val tests = Tests{
    test("Compress2Main") - withServer(Compress2Main) { host =>
      val expected = "Hello World! Hello World! Hello World!"
      requests.get(s"$host").text() ==> expected
      assert(
        requests.get(s"$host", autoDecompress = false).text().length < expected.length
      )
    }
  }
}
