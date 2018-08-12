package app
import io.undertow.Undertow

import utest._

object ExampleTests extends TestSuite{
  def test[T](example: cask.main.BaseMain)(f: String => T): T = {
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
    'Endpoints - test(Endpoints){ host =>
      requests.get(s"$host/echo/200").text() ==> "Echo 200"
      requests.get(s"$host/echo/200").statusCode ==> 200
      requests.get(s"$host/echo/400").text() ==> "Echo 400"
      requests.get(s"$host/echo/400").statusCode ==> 400
    }
  }
}
