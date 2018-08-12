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
    'MinimalApplication2 - test(MinimalMain){ host =>
      val success = requests.get(host)

      success.text() ==> "Hello World!"
      success.statusCode ==> 200

      requests.get(s"$host/doesnt-exist").statusCode ==> 404

      requests.post(s"$host/do-thing", data = "hello").text() ==> "olleh"

      requests.get(s"$host/do-thing").statusCode ==> 404
    }
  }
}
