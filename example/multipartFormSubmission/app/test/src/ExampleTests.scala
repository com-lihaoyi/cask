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
    test("MultipartFormSubmission") - withServer(MultipartFormSubmission){ host =>
      val classPath = System.getProperty("java.class.path", ".");
      val elements = classPath.split(System.getProperty("path.separator"));
      elements.filter(e => e.endsWith("/app/resources")).headOption.map(resourcePath => {
        val withFile = requests.post(s"$host/post", data = requests.MultiPart(
          requests.MultiItem("somefile", new java.io.File(s"$resourcePath/example.txt"), "example.txt"),
        ))
        withFile.text() ==> s"filename: example.txt"
        withFile.statusCode ==> 200

        val withoutFile = requests.post(s"$host/post", data = requests.MultiPart(
          requests.MultiItem("somefile", Array[Byte]()),
        ))
        withoutFile.text() ==> s"filename: null"
        withoutFile.statusCode ==> 200
      }).isDefined ==> true
    }
  }
}
