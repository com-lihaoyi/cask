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
    test("Cookies") - withServer(Cookies){ host =>
      val sess = requests.Session()
      sess.get(s"$host/read-cookie").statusCode ==> 400
      sess.get(s"$host/store-cookie")
      sess.get(s"$host/read-cookie").text() ==> "the username"
      sess.get(s"$host/read-cookie").statusCode ==> 200
      sess.get(s"$host/delete-cookie")
      sess.get(s"$host/read-cookie").statusCode ==> 400

    }
  }
}
