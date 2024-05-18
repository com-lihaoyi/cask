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
    test("FormJsonPost") - withServer(FormJsonPost){ host =>
      val response1 = requests.post(s"$host/json", data = """{"value1": true, "value2": [3]}""")
      assert(
        ujson.read(response1.text()) == ujson.Str("OK true List(3)") ||
        ujson.read(response1.text()) == ujson.Str("OK true Vector(3)")
      )

      val response2 = requests.post(
        s"$host/json-obj",
        data = """{"value1": true, "value2": [3]}"""
      )
      ujson.read(response2.text()) ==> ujson.Obj("value1" -> true, "value2" -> ujson.Arr(3))


      val response2Cached = requests.post(
        s"$host/json-obj-cached",
        data = """{"value1": true, "value2": [3]}"""
      )
      ujson.read(response2Cached.text()) ==>
        ujson.Obj(
          "value1" -> true,
          "value2" -> ujson.Arr(3),
          "body" -> """{"value1": true, "value2": [3]}"""
        )


      val response3 = requests.post(
        s"$host/form",
        data = Seq("value1" -> "hello", "value2" -> "1", "value2" -> "2")
      )
      assert(
        response3.text() == "OK FormValue(hello,null) List(1, 2)" ||
        response3.text() == "OK FormValue(hello,null) Vector(1, 2)"
      )

      val response4 = requests.post(
        s"$host/form-obj",
        data = Seq("value1" -> "hello", "value2" -> "1", "value2" -> "2")
      )
      ujson.read(response4.text()) ==> ujson.Obj("value1" -> "hello", "value2" -> ujson.Arr(1, 2))

      val response5 = requests.post(
        s"$host/upload",
        data = requests.MultiPart(
          requests.MultiItem("image", "...", "my-best-image.txt")
        )
      )
      response5.text() ==> "my-best-image.txt"


      val response6 = requests.post(
        s"$host/json-extra/omg/wtf/bbq?iam=cow&hearme=moo",
        data = """{"value1": true, "value2": [3]}"""
      )

      val text6 = response6.text()
      assert(
        text6 == "\"OK true List(3) Map(hearme -> ArraySeq(moo), iam -> ArraySeq(cow)) List(omg, wtf, bbq)\"" ||
        text6 == "\"OK true Vector(3) Map(hearme -> WrappedArray(moo), iam -> WrappedArray(cow)) List(omg, wtf, bbq)\""
      )

      val response7 = requests.post(
        s"$host/form-extra/omg/wtf/bbq?iam=cow&hearme=moo",
        data = Seq("value1" -> "hello", "value2" -> "1", "value2" -> "2")
      )

      val text7 = response7.text()
      assert(
        text7 == "OK FormValue(hello,null) List(1, 2) Map(hearme -> ArraySeq(moo), iam -> ArraySeq(cow)) List(omg, wtf, bbq)" ||
        text7 == "OK FormValue(hello,null) List(1, 2) Map(hearme -> WrappedArray(moo), iam -> WrappedArray(cow)) List(omg, wtf, bbq)"
      )
    }
  }
}
