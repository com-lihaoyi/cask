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


      assert(
        requests.get(s"$host/article/123?param=xyz").text() ==
          "Article 123 xyz"
      )

      requests.get(s"$host/article/123", check = false).text() ==>
        """Missing argument: (param: String)
          |
          |Arguments provided did not match expected signature:
          |
          |getArticle
          |  articleId  Int
          |  param  String
          |
          |""".stripMargin

      assert(
        requests.get(s"$host/article2/123?param=xyz").text() ==
          "Article 123 Some(xyz)"
      )

      assert(
        requests.get(s"$host/article2/123").text() ==
          "Article 123 None"
      )

      assert(
        requests.get(s"$host/article3/123?param=xyz").text() ==
          "Article 123 xyz"
      )

      assert(
        requests.get(s"$host/article3/123").text() ==
          "Article 123 DEFAULT VALUE"
      )


      assert(
        requests.get(s"$host/article4/123?param=xyz&param=abc").text() ==
          "Article 123 ArraySeq(xyz, abc)" ||
        requests.get(s"$host/article4/123?param=xyz&param=abc").text() ==
          "Article 123 ArrayBuffer(xyz, abc)"
      )

      requests.get(s"$host/article4/123", check = false).text() ==>
        """Missing argument: (param: Seq[String])
          |
          |Arguments provided did not match expected signature:
          |
          |getArticleSeq
          |  articleId  Int
          |  param  Seq[String]
          |
          |""".stripMargin

      assert(
        requests.get(s"$host/article5/123?param=xyz&param=abc").text() ==
          "Article 123 ArraySeq(xyz, abc)" ||
        requests.get(s"$host/article5/123?param=xyz&param=abc").text() ==
          "Article 123 ArrayBuffer(xyz, abc)"
      )
      assert(
        requests.get(s"$host/article5/123").text() == "Article 123 List()"
      )

      requests.get(s"$host/path/one/two/three").text() ==>
        "Subpath List(one, two, three)"

      requests.post(s"$host/path/one/two/three").text() ==>
        "POST Subpath List(one, two, three)"
    }

  }
}
