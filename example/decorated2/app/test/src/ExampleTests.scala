package app
import io.undertow.Undertow

import utest._

object ExampleTests extends TestSuite{
  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = Undertow.builder
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
    test("Decorated2") - withServer(Decorated2){ host =>
      requests.get(s"$host/hello/woo").text() ==> "woo31337"
      requests.get(s"$host/internal-extra/goo").text() ==> "goo[haoyi]31337"
      requests.get(s"$host/ignore-extra/boo").text() ==> "boo[haoyi]"

    }
  }
}
