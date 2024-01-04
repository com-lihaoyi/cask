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
    test("QueryParams") - withServer(QueryParams){ host =>
      val noIndexPage = requests.get(host, check = false)
      noIndexPage.statusCode ==> 404

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


      val res1 = requests.get(s"$host/article4/123?param=xyz&param=abc").text()
      assert(
        res1 == "Article 123 ArraySeq(xyz, abc)" ||
        res1 == "Article 123 ArrayBuffer(xyz, abc)"
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

      val res2 = requests.get(s"$host/article5/123?param=xyz&param=abc").text()
      assert(
        res2 == "Article 123 ArraySeq(xyz, abc)" ||
        res2 == "Article 123 ArrayBuffer(xyz, abc)"
      )
      assert(
        requests.get(s"$host/article5/123").text() == "Article 123 List()"
      )

      val res3 = requests.get(s"$host/user2/lihaoyi?unknown1=123&unknown2=abc", check = false).text()
      assert(
        res3 == "User lihaoyi Map(unknown1 -> ArrayBuffer(123), unknown2 -> ArrayBuffer(abc))" ||
        res3 == "User lihaoyi Map(unknown1 -> WrappedArray(123), unknown2 -> WrappedArray(abc))" ||
        res3 == "User lihaoyi Map(unknown1 -> ArraySeq(123), unknown2 -> ArraySeq(abc))"
      )
    }
  }
}
