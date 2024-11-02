package test.cask

import cask.model.Request
import utest._

object FailureTests3 extends TestSuite {
  val tests = Tests{
    "returnType" - {
      utest.compileError("""
        object Routes extends cask.MainRoutes{
          @cask.get("/foo")
          def hello(world: String) = (1, 1)
          initialize()
        }
      """).msg ==>
        "error in route definition `def hello` (at tasty-reflect:4:15): the method's return type scala.Tuple2[scala.Int, scala.Int] cannot be converted to the expected response type cask.model.Response[cask.model.Response.Data]"
    }
  }
}
