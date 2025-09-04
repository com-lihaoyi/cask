package test.cask

import cask.model.Request
import utest._

object FailureTests extends TestSuite {
  class myDecorator extends cask.RawDecorator {
    def wrapFunction(ctx: Request, delegate: Delegate) = {
      delegate(ctx, Map("extra" -> 31337))
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
      assert(m.contains("required: cask.router.Decorator[_, cask.endpoints.WebsocketResult, _, _]"))
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
      val msg = utest.compileError("""
        object Decorated extends cask.MainRoutes{
          @cask.get("/hello/:world")
          @cask.get("/hello/:world")
          def hello(world: String)(extra: Int)= world
          initialize()
        }
      """).msg.replaceAllLiterally("cask.endpoints.get", "cask.get") // com-lihaoyi/cask#171
      assert(msg == "You can only apply one Endpoint annotation to a function, not 2 in cask.get, cask.get")
    }
  }
}
