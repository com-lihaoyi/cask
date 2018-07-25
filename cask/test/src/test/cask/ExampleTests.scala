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
    }
    'VariableRoutes - test(VariableRoutes){ host =>
      val noIndexPage = requests.get(host)
      noIndexPage.statusCode ==> 404

      requests.get(host + "/user/lihaoyi").text() ==> "User lihaoyi"

      requests.get(host + "/user").statusCode ==> 404


      requests.get(host + "/post/123?query=xyz&query=abc").text() ==>
        "Post 123 ArrayBuffer(xyz, abc)"

      requests.get(host + "/post/123").text() ==>
        """Missing argument: (query: Seq[String])
          |
          |Arguments provided did not match expected signature:
          |
          |showPost
          |  postId  Int
          |  query  Seq[String]
          |
          |""".stripMargin

      requests.get(host + "/path/one/two/three").text() ==>
        "Subpath List(one, two, three)"
    }

    'StaticFiles - test(StaticFiles){ host =>
      requests.get(host + "/static/example.txt").text() ==>
        "the quick brown fox jumps over the lazy dog"
    }

    'RedirectAbort - test(RedirectAbort){ host =>
      val resp = requests.get(host + "/")
      resp.statusCode ==> 401
      resp.history.get.statusCode ==> 301
    }
  }
}
