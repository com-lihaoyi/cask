package test.cask
import cask.internal.DispatchTrie
import utest._

object DispatchTrieTests extends TestSuite {
  val tests = Tests{

    "hello" - {
      val x = DispatchTrie.construct(0,
        Seq((Vector("hello"), 1, false))
      )(Seq(_))

      assert(
        x.lookup(List("hello"), Vector()) == Some((1, Map(), Nil)),
        x.lookup(List("hello", "world"), Vector()) == None,
        x.lookup(List("world"), Vector()) == None
      )
    }
    "nested" - {
      val x = DispatchTrie.construct(0,
        Seq(
          (Vector("hello", "world"), 1, false),
          (Vector("hello", "cow"), 2, false)
        )
      )(Seq(_))
      assert(
        x.lookup(List("hello", "world"), Vector()) == Some((1, Map(), Nil)),
        x.lookup(List("hello", "cow"), Vector()) == Some((2, Map(), Nil)),
        x.lookup(List("hello"), Vector()) == None,
        x.lookup(List("hello", "moo"), Vector()) == None,
        x.lookup(List("hello", "world", "moo"), Vector()) == None
      )
    }
    "bindings" - {
      val x = DispatchTrie.construct(0,
        Seq((Vector(":hello", ":world"), 1, false))
      )(Seq(_))
      assert(
        x.lookup(List("hello", "world"), Vector()) == Some((1, Map("hello" -> "hello", "world" -> "world"), Nil)),
        x.lookup(List("world", "hello"), Vector()) == Some((1, Map("hello" -> "world", "world" -> "hello"), Nil)),

        x.lookup(List("hello", "world", "cow"), Vector()) == None,
        x.lookup(List("hello"), Vector()) == None
      )
    }

    "path" - {
      val x = DispatchTrie.construct(0,
        Seq((Vector("hello"), 1, true))
      )(Seq(_))

      assert(
        x.lookup(List("hello", "world"), Vector()) ==  Some((1,Map(), Seq("world"))),
        x.lookup(List("hello", "world", "cow"), Vector()) ==  Some((1,Map(), Seq("world", "cow"))),
        x.lookup(List("hello"), Vector()) == Some((1,Map(), Seq())),
        x.lookup(List(), Vector()) == None
      )
    }

    "wildcards" - {
      test - {
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", ":world"), 1, false),
            (Vector("hello", "world"),  1, false)
          )
        )(Seq(_))
      }
      test - {
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", ":world"), 1, false),
            (Vector("hello", "world", "omg"), 2, false)
          )
        )(Seq(_))
      }
    }
    "errors" - {
      test - {
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello"), 1, true),
            (Vector("hello", "cow", "omg"), 2, false)
          )
        )(Seq(_))

        val ex = intercept[Exception]{
          DispatchTrie.construct(0,
            Seq(
              (Vector("hello"), 1, true),
              (Vector("hello", "cow", "omg"), 1, false)
            )
          )(Seq(_))
        }

        assert(
          ex.getMessage ==
          "Routes overlap with subpath capture: 1 /hello, 1 /hello/cow/omg"
        )
      }
      test - {
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", ":world"), 1, false),
            (Vector("hello", ":cow"), 2, false)
          )
        )(Seq(_))

        val ex = intercept[Exception]{
          DispatchTrie.construct(0,
            Seq(
              (Vector("hello", ":world"), 1, false),
              (Vector("hello", ":cow"), 1, false)
            )
          )(Seq(_))
        }

        assert(
          ex.getMessage ==
          "More than one endpoint has the same path: 1 /hello/:world, 1 /hello/:cow"
        )
      }
      test - {
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", "world"), 1, false),
            (Vector("hello", "world"), 2, false)
          )
        )(Seq(_))

        val ex = intercept[Exception]{
          DispatchTrie.construct(0,
            Seq(
              (Vector("hello", "world"), 1, false),
              (Vector("hello", "world"), 1, false)
            )
          )(Seq(_))
        }
        assert(
          ex.getMessage ==
          "More than one endpoint has the same path: 1 /hello/world, 1 /hello/world"
        )
      }
    }
  }
}
