package test.cask

import cask.internal.Router
import cask.model.{ParamContext, Response}
import utest._

object FailureTests extends TestSuite {
  class myDecorator extends cask.Decorator {
    def wrapFunction(ctx: ParamContext, delegate: Delegate): Returned = {
      delegate(Map("extra" -> 31337))
    }
  }
  val tests = Tests{
    'mismatchedDecorators - {
      utest.compileError("""
        object Decorated extends cask.MainRoutes{
          @cask.get("/hello/:world")
          @myDecorator()
          def hello(world: String)(extra: Int)= world
          initialize()
        }
      """).msg ==>
        "Last annotation applied to a function must be an instance of Endpoint, not test.cask.FailureTests.myDecorator"

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

