package cask.actor
import utest._
object ActorsTest extends TestSuite{
  def tests = Tests{
    test("hello"){
      import Context.Simple.global

      sealed trait Msg

      object logger extends SimpleActor[Msg]{
        def run(msg: Msg) = {

        }
      }
    }
  }
}