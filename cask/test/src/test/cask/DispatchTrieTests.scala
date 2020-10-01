package test.cask
import cask.internal.DispatchTrie
import utest._

object DispatchTrieTests extends TestSuite {
  val tests = Tests{

    "hello" - {
      val x = DispatchTrie.construct(0,
        Seq((Vector("hello"), 1, false))
      )

      assert(
        x.lookup(List("hello"), Map()) == Some((1, Map(), Nil)),
        x.lookup(List("hello", "world"), Map()) == None,
        x.lookup(List("world"), Map()) == None
      )
    }
    "nested" - {
      val x = DispatchTrie.construct(0,
        Seq(
          (Vector("hello", "world"), 1, false),
          (Vector("hello", "cow"), 2, false)
        )
      )
      assert(
        x.lookup(List("hello", "world"), Map()) == Some((1, Map(), Nil)),
        x.lookup(List("hello", "cow"), Map()) == Some((2, Map(), Nil)),
        x.lookup(List("hello"), Map()) == None,
        x.lookup(List("hello", "moo"), Map()) == None,
        x.lookup(List("hello", "world", "moo"), Map()) == None
      )
    }
    "bindings" - {
      val x = DispatchTrie.construct(0,
        Seq((Vector(":hello", ":world"), 1, false))
      )
      assert(
        x.lookup(List("hello", "world"), Map()) == Some((1, Map("hello" -> "hello", "world" -> "world"), Nil)),
        x.lookup(List("world", "hello"), Map()) == Some((1, Map("hello" -> "world", "world" -> "hello"), Nil)),

        x.lookup(List("hello", "world", "cow"), Map()) == None,
        x.lookup(List("hello"), Map()) == None
      )
    }

    "path" - {
      val x = DispatchTrie.construct(0,
        Seq((Vector("hello"), 1, true))
      )

      assert(
        x.lookup(List("hello", "world"), Map()) ==  Some((1,Map(), Seq("world"))),
        x.lookup(List("hello", "world", "cow"), Map()) ==  Some((1,Map(), Seq("world", "cow"))),
        x.lookup(List("hello"), Map()) == Some((1,Map(), Seq())),
        x.lookup(List(), Map()) == None
      )
    }

    "errors" - {
      intercept[Exception]{
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", ":world"), 1, false),
            (Vector("hello", "world"),  2, false)
          )
        )
      }
      intercept[Exception]{
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", ":world"), 1, false),
            (Vector("hello", "world", "omg"), 2, false)
          )
        )
      }
      intercept[Exception]{
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello"), 1, true),
            (Vector("hello", "cow", "omg"), 2, false)
          )
        )
      }
      intercept[Exception]{
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", ":world"), 1, false),
            (Vector("hello", ":cow"), 2, false)
          )
        )
      }
      intercept[Exception]{
        DispatchTrie.construct(0,
          Seq(
            (Vector("hello", "world"), 1, false),
            (Vector("hello", "world"), 2, false)
          )
        )
      }
    }
  }
}
