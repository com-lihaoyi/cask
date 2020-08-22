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

  val tests = Tests {
    test("ExtraParams") - withServer(ExtraParams) { host =>
      requests.get(s"$host/echo/strict?param1=hello").text() ==> "hello"
      requests.get(s"$host/echo/strict?param1=hello&param2=world", check = false).statusCode ==> 400

      requests.get(s"$host/echo/lax?param1=hello").text() ==> "param1=hello"
      requests.get(s"$host/echo/lax?param1=hello&param2=world").text() ==>
        "param1=hello&param2=world"
    }
  }
}
