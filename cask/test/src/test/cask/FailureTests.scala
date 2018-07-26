package test.cask

import cask.model.ParamContext
import utest._

object FailureTests extends TestSuite {
  class myDecorator extends cask.Routes.Decorator {
    def getRawParams(ctx: ParamContext) = Right(Map("extra" -> 31337))
  }
  val tests = Tests{

    'mismatchedDecorators - {
      utest.compileError("""
        object Decorated extends cask.MainRoutes{
          @cask.get("/hello/:world")
          def hello(world: String)(extra: Int) = world + extra
          initialize()
        }
      """).msg ==>
      "Endpoint hello's number of parameter lists (2) doesn't match number of decorators (1)"

      utest.compileError("""
        object Decorated extends cask.MainRoutes{
          @myDecorator()
          @cask.get("/hello/:world")
          def hello(world: String)= world
          initialize()
        }
      """).msg ==>
        "Endpoint hello's number of parameter lists (1) doesn't match number of decorators (2)"

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
