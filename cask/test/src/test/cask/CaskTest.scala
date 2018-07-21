package test.cask
import cask.DispatchTrie
import utest._

object CaskTest extends TestSuite {
  val tests = Tests{

    'hello - {
      val x = DispatchTrie.construct(0,
        Seq(Vector("hello") -> 1)
      )

      assert(
        x.lookup(List("hello"), Map()) == Some((1, Map())),
        x.lookup(List("hello", "world"), Map()) == None,
        x.lookup(List("world"), Map()) == None
      )
    }
    'nested - {
      val x = DispatchTrie.construct(0,
        Seq(
          Vector("hello", "world") -> 1,
          Vector("hello", "cow") -> 2
        )
      )
      assert(
        x.lookup(List("hello", "world"), Map()) == Some((1, Map())),
        x.lookup(List("hello", "cow"), Map()) == Some((2, Map())),
        x.lookup(List("hello"), Map()) == None,
        x.lookup(List("hello", "moo"), Map()) == None,
        x.lookup(List("hello", "world", "moo"), Map()) == None
      )
    }
    'bindings - {
      val x = DispatchTrie.construct(0,
        Seq(Vector(":hello", ":world") -> 1)
      )
      assert(
        x.lookup(List("hello", "world"), Map()) == Some((1, Map("hello" -> "hello", "world" -> "world"))),
        x.lookup(List("world", "hello"), Map()) == Some((1, Map("hello" -> "world", "world" -> "hello"))),

        x.lookup(List("hello", "world", "cow"), Map()) == None,
        x.lookup(List("hello"), Map()) == None
      )
    }

    'path - {
      val x = DispatchTrie.construct(0,
        Seq(Vector("hello", "::world") -> 1)
      )
      assert(
        x.lookup(List("hello", "world"), Map()) ==  Some((1,Map("world" -> "world"))),
        x.lookup(List("hello", "world", "cow"), Map()) ==  Some((1,Map("world" -> "world/cow"))),
        x.lookup(List("hello"), Map()) == None
      )
    }

    'errors - {
      intercept[Exception]{
        DispatchTrie.construct(0,
          Seq(
            Vector("hello", ":world") -> 1,
            Vector("hello", "world") -> 2
          )
        )
      }
      intercept[Exception]{
        DispatchTrie.construct(0,
          Seq(
            Vector("hello", ":world") -> 1,
            Vector("hello", "world", "omg") -> 2
          )
        )
      }
      intercept[Exception]{
        DispatchTrie.construct(0,
          Seq(
            Vector("hello", "::world") -> 1,
            Vector("hello", "cow", "omg") -> 2
          )
        )
      }
      intercept[Exception]{
        DispatchTrie.construct(0,
          Seq(
            Vector("hello", ":world") -> 1,
            Vector("hello", ":cow") -> 2
          )
        )
      }
      intercept[Exception]{
        DispatchTrie.construct(0,
          Seq(
            Vector("hello", "world") -> 1,
            Vector("hello", "world") -> 2
          )
        )
      }
    }
  }
}