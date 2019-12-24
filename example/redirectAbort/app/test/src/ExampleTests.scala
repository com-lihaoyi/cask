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

  val tests = Tests{

    test("RedirectAbort") - withServer(RedirectAbort){ host =>
      val resp = requests.get(s"$host/", check = false)
      resp.statusCode ==> 401
      resp.history.get.statusCode ==> 301
    }
  }
}
