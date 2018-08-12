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

    'RedirectAbort - test(RedirectAbort){ host =>
      val resp = requests.get(s"$host/")
      resp.statusCode ==> 401
      resp.history.get.statusCode ==> 301
    }
  }
}
