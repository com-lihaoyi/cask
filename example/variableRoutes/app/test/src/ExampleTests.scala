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
    test("VariableRoutes") - withServer(VariableRoutes){ host =>
      val noIndexPage = requests.get(host, check = false)
      noIndexPage.statusCode ==> 404

      requests.get(s"$host/user/lihaoyi").text() ==> "User lihaoyi"

      requests.get(s"$host/user", check = false).statusCode ==> 404

      requests.get(s"$host/path/one/two/three").text() ==>
        "Subpath List(one, two, three)"

      requests.post(s"$host/path/one/two/three").text() ==>
        "POST Subpath List(one, two, three)"

      requests.get(s"$host/user/lihaoyi?unknown1=123&unknown2=abc", check = false).text() ==>
        """Unknown arguments: "unknown1" "unknown2"
          |
          |Arguments provided did not match expected signature:
          |
          |getUserProfile
          |  userName  String
          |
          |""".stripMargin
    }
  }
}
