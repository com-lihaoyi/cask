package test.cask
import cask.internal.Router.ArgReader
import cask.model.ParamContext


object Decorator extends cask.MainRoutes{
  class myDecorator extends cask.main.Routes.Decorator {
    type InputType = Int

    def handle(ctx: ParamContext) = Map("extra" -> 31337)

    def parseMethodInput[T] = new ArgReader[Int, T, ParamContext] {
      def arity = 1

      def read(ctx: ParamContext, label: String, input: Int) = input.asInstanceOf[T]
    }
  }

  @myDecorator()
  @cask.get("/hello/:world")
  def hello(world: String)(extra: Int) = {
    world + extra
  }

  initialize()
}
