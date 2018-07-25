package test.cask
import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import utest._

object ExampleTests extends TestSuite{
  def test[T](example: cask.main.MainRoutes)(f: String => T): T = {
    val server = Undertow.builder
      .addHttpListener(8080, "localhost")
      .setHandler(new BlockingHandler(example.defaultHandler))
      .build
    server.start()
    val res = f("http://localhost:8080")
    server.stop()
    res
  }
  val tests = Tests{
    'MinimalApplication - test(MinimalApplication){ host =>
      val success = requests.get(host)

      success.text() ==> "Hello World!"
      success.statusCode ==> 200

      val failure = requests.get(host + "/doesnt-exist")
      failure.text() ==> "Error 404: Not Found"
      failure.statusCode ==> 404

      val successInfo = requests.get(host + "/request-info?my-query-param=my-query-value")
      assert(
        successInfo.text().contains("my-query-param"),
        successInfo.text().contains("my-query-value")
      )
      successInfo.statusCode ==> 200
    }
    'VariableRoutes - test(VariableRoutes){ host =>
      val noIndexPage = requests.get(host)
      noIndexPage.statusCode ==> 404

      val userPage = requests.get(host + "/user/lihaoyi")
      userPage.text() ==> "User lihaoyi"
      userPage.statusCode ==> 200

      val badUserPage = requests.get(host + "/user")
      badUserPage.statusCode ==> 404

      val postPage = requests.get(host + "/post/123?query=xyz&query=abc")
      postPage.text() ==> "Post 123 ArrayBuffer(xyz, abc)"
      userPage.statusCode ==> 200

      val badPostPage = requests.get(host + "/post/123")
      badPostPage.text()
    }
  }
}
