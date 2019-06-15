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
    'FormJsonPost - test(FormJsonPost){ host =>
      requests.post(s"$host/json", data = """{"value1": true, "value2": [3]}""").text() ==>
        "OK true List(3)"

      requests.post(
        s"$host/form",
        data = Seq("value1" -> "hello", "value2" -> "1", "value2" -> "2")
      ).text() ==>
        "OK FormValue(hello,null) List(1, 2)"

      val resp = requests.post(
        s"$host/upload",
        data = requests.MultiPart(
          requests.MultiItem("image", "...", "my-best-image.txt")
        )
      )
      resp.text() ==> "my-best-image.txt"
    }
  }
}
