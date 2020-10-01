package test.cask

import cask.model.Request
import utest._

object FailureTests extends TestSuite {
  class myDecorator extends cask.RawDecorator {
    def wrapFunction(ctx: Request, delegate: Delegate) = {
      delegate(Map("extra" -> 31337))
    }
  }

  val tests = Tests{
    "mismatchedDecorators" - {
      val m = utest.compileError("""
        object Decorated extends cask.MainRoutes{
          @myDecorator
          @cask.websocket("/hello/:world")
          def hello(world: String)(extra: Int) = ???
          initialize()
        }
      """).msg
      assert(m.contains("required: cask.router.Decorator[_, cask.endpoints.WebsocketResult, _]"))
    }

    "noEndpoint" - {
      utest.compileError("""
        object Decorated extends cask.MainRoutes{
          @cask.get("/hello/:world")
          @myDecorator()
          def hello(world: String)(extra: Int)= world
          initialize()
        }
      """).msg ==>
        "Last annotation applied to a function must be an instance of Endpoint, not test.cask.FailureTests.myDecorator"
      }

    "tooManyEndpoint" - {
      utest.compileError("""
        object Decorated extends cask.MainRoutes{
          @cask.get("/hello/:world")
          @cask.get("/hello/:world")
          def hello(world: String)(extra: Int)= world
          initialize()
        }
      """).msg ==>
        "You can only apply one Endpoint annotation to a function, not 2 in cask.endpoints.get, cask.endpoints.get"
    }
  }
}
