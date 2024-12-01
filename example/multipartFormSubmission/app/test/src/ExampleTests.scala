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
    test("MultipartFormSubmission") - withServer(MultipartFormSubmission) { host =>
      val withFile = requests.post(s"$host/post", data = requests.MultiPart(
        requests.MultiItem("somefile", Array[Byte](1,2,3,4,5) , "example.txt"),
      ))
      withFile.text() ==> s"filename: example.txt"
      withFile.statusCode ==> 200

      val withoutFile = requests.post(s"$host/post", data = requests.MultiPart(
        requests.MultiItem("somefile", Array[Byte]()),
      ))
      withoutFile.text() ==> s"filename: null"
      withoutFile.statusCode ==> 200
    }
  }
}
