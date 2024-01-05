package test.cask
import cask.internal.DispatchTrie
import utest._

object DispatchTrieTests extends TestSuite {
  val tests = Tests{

    test("hello") {
      val x = DispatchTrie.construct(0,
        Seq((Vector("hello"), "GET", false))
      )(Seq(_))

      x.lookup(List("hello"), Map()) ==> Some(("GET", Map(), Nil))

      x.lookup(List("hello", "world"), Map()) ==> None
      x.lookup(List("world"), Map()) ==> None
    }
    test("nested") {
      val x = DispatchTrie.construct(0,
        Seq(
          (Vector("hello", "world"), "GET", false),
          (Vector("hello", "cow"), "POST", false)
        )
      )(Seq(_))

      x.lookup(List("hello", "world"), Map()) ==> Some(("GET", Map(), Nil))
      x.lookup(List("hello", "cow"), Map()) ==> Some(("POST", Map(), Nil))

      x.lookup(List("hello"), Map()) ==> None
      x.lookup(List("hello", "moo"), Map()) ==> None
      x.lookup(List("hello", "world", "moo"), Map()) ==> None

    }
    test("bindings") {
      val x = DispatchTrie.construct(0,
        Seq((Vector(":hello", ":world"), "GET", false))
      )(Seq(_))

      x.lookup(List("hello", "world"), Map()) ==> Some(("GET", Map("hello" -> "hello", "world" -> "world"), Nil))
      x.lookup(List("world", "hello"), Map()) ==> Some(("GET", Map("hello" -> "world", "world" -> "hello"), Nil))

      x.lookup(List("hello", "world", "cow"), Map()) ==> None
      x.lookup(List("hello"), Map()) ==> None

    }

    test("path") {
      val x = DispatchTrie.construct(0,
        Seq((Vector("hello"), "GET", true))
      )(Seq(_))

      x.lookup(List("hello", "world"), Map()) ==>  Some(("GET", Map(), Seq("world")))
      x.lookup(List("hello", "world", "cow"), Map()) ==>  Some(("GET", Map(), Seq("world", "cow")))
      x.lookup(List("hello"), Map()) ==> Some(("GET", Map(), Seq()))

      x.lookup(List(), Map()) == None
    }

    test("partialOverlap") {
      test("wildcardAndFixedWildcard"){
        val x = DispatchTrie.construct(0,
          Seq(
            (Vector(":hello"), "GET", false),
            (Vector("hello", ":world"), "GET", false)
          )
        )(Seq(_))

        x.lookup(List("hello", "world"), Map()) ==> Some(("GET", Map("hello" -> "hello", "world" -> "world"), Nil))
        x.lookup(List("world", "hello"), Map()) ==> Some(("GET", Map("hello" -> "world", "world" -> "hello"), Nil))

        x.lookup(List("hello", "world", "cow"), Map()) ==> None
        x.lookup(List("hello"), Map()) ==> None
      }


      test("wildcardAndWildcardFixed") {
        val x = DispatchTrie.construct(0,
          Seq(
            (Vector(":hello"), "GET", false),
            (Vector(":hello", "world"), "GET", false)
          )
        )(Seq(_))

        x.lookup(List("hello", "world"), Map()) ==> Some(("GET", Map("hello" -> "hello"), Nil))
        x.lookup(List("hello"), Map()) ==> Some(("GET", Map("hello" -> "hello"), Nil))

        x.lookup(List("world", "hello"), Map()) ==> None
        x.lookup(List("hello", "world", "cow"), Map()) ==> None
      }

      test("sameWildcardDifferingFixed"){
        val x = DispatchTrie.construct(0,
          Seq(
            (Vector(":var", "foo"), ("GET", "fooImpl"), false),
            (Vector(":var", "bar"), ("GET", "barImpl"), false)
          )
        )(t => Seq(t._1))

        x.lookup(List("hello", "foo"), Map()) ==> Some((("GET", "fooImpl"), Map("var" -> "hello"), Nil))
        x.lookup(List("world", "bar"), Map()) ==> Some((("GET", "barImpl"), Map("var" -> "world"), Nil))

        x.lookup(List("hello", "world", "cow"), Map()) ==> None
        x.lookup(List("hello"), Map()) ==> None
      }

      test("differingWildcardDifferingFixed") {
        val x = DispatchTrie.construct(0,
          Seq(
            (Vector(":hello", "foo"), "GET", false),
            (Vector(":world", "bar"), "GET", false)
          )
        )(Seq(_))

        x.lookup(List("hello", "world"), Map()) ==> Some(("GET", Map("hello" -> "hello", "world" -> "world"), Nil))
        x.lookup(List("world", "hello"), Map()) ==> Some(("GET", Map("hello" -> "world", "world" -> "hello"), Nil))

        x.lookup(List("hello", "world", "cow"), Map()) ==> None
        x.lookup(List("hello"), Map()) ==> None
      }

    }


    test("errors") {
      test("wildcardAndFixed") {
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", ":world"), "GET", false),
            (Vector("hello", "world"),  "POST", false)
          )
        )(Seq(_))

        val ex = intercept[Exception]{
          DispatchTrie.construct(0,
            Seq(
              (Vector("hello", ":world"), "GET", false),
              (Vector("hello", "world"),  "GET", false)
            )
          )(Seq(_))
        }

        assert(
          ex.getMessage ==
          "Routes overlap with wildcards: GET /hello/:world, GET /hello/world"
        )
      }
      test("subpathCapture") {
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello"), "GET", true),
            (Vector("hello", "cow", "omg"), "POST", false)
          )
        )(Seq(_))

        val ex = intercept[Exception]{
          DispatchTrie.construct(0,
            Seq(
              (Vector("hello"), "GET", true),
              (Vector("hello", "cow", "omg"), "GET", false)
            )
          )(Seq(_))
        }

        assert(
          ex.getMessage ==
          "Routes overlap with subpath capture: GET /hello, GET /hello/cow/omg"
        )
      }
      test("wildcardAndWildcard") {
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", ":world"), "GET", false),
            (Vector("hello", ":cow"), "POST", false)
          )
        )(Seq(_))

        val ex = intercept[Exception]{
          DispatchTrie.construct(0,
            Seq(
              (Vector("hello", ":world"), "GET", false),
              (Vector("hello", ":cow"), "GET", false)
            )
          )(Seq(_))
        }

        assert(
          ex.getMessage ==
          "Routes overlap with wildcards: GET /hello/:world, GET /hello/:cow"
        )
      }
      test("wildcardAndWildcardPrefix") {
        DispatchTrie.construct(0,
          Seq(
            (Vector(":world", "hello"), "GET", false),
            (Vector(":cow", "hello"), "POST", false)
          )
        )(Seq(_))

        val ex = intercept[Exception]{
          DispatchTrie.construct(0,
            Seq(
              (Vector(":world", "hello"), "GET", false),
              (Vector(":cow", "hello"), "GET", false)
            )
          )(Seq(_))
        }

        assert(
          ex.getMessage ==
          "Routes overlap with wildcards: GET /:world/hello, GET /:cow/hello"
        )
      }
      test("fixedAndFixed") {
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", "world"), "GET", false),
            (Vector("hello", "world"), "POST", false)
          )
        )(Seq(_))

        val ex = intercept[Exception]{
          DispatchTrie.construct(0,
            Seq(
              (Vector("hello", "world"), "GET", false),
              (Vector("hello", "world"), "GET", false)
            )
          )(Seq(_))
        }
        assert(
          ex.getMessage ==
          "More than one endpoint has the same path: GET /hello/world, GET /hello/world"
        )
      }
    }
  }
}
