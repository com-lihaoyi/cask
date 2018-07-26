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
    val res =
      try f("http://localhost:8080")
      finally server.stop()
    res
  }

  val tests = Tests{
    'MinimalApplication - test(MinimalApplication){ host =>
      val success = requests.get(host)

      success.text() ==> "Hello World!"
      success.statusCode ==> 200

      requests.get(host + "/doesnt-exist").statusCode ==> 404

      requests.post(host + "/do-thing", data = "hello").text() ==> "olleh"

      requests.get(host + "/do-thing").statusCode ==> 404
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

    'MultipartFileUploads - test(MultipartFileUploads){ host =>
      val resp = requests.post(
        host + "/upload",
        data = requests.MultiPart(
          requests.MultiItem("image", "...", "my-best-image.txt")
        )
      )
      resp.text() ==> "my-best-image.txt"
    }
    'FormJsonPost - test(FormJsonPost){ host =>
      requests.post(host + "/json", data = """{"value1": true, "value2": [3]}""").text() ==>
        "OK true Vector(3)"

      requests.post(
        host + "/form",
        data = Seq("value1" -> "hello", "value2" -> "1", "value2" -> "2")
      ).text() ==>
      "OK Plain(hello,null) List(1, 2)"
    }
    'Decorated - test(Decorated){ host =>
      requests.get(host + "/hello/woo").text() ==> "woo31337"
      requests.get(host + "/internal/boo").text() ==> "boo[haoyi]"
      requests.get(host + "/internal-extra/goo").text() ==> "goo[haoyi]31337"

    }
  }
}
