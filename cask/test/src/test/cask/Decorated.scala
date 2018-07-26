package test.cask
import cask.model.ParamContext


object Decorated extends cask.MainRoutes{
  class myDecorator extends cask.Routes.Decorator {
    def getParamValues(ctx: ParamContext) = Map("extra" -> 31337)
  }

  @myDecorator()
  @cask.get("/hello/:world")
  def hello(world: String)(extra: Int) = {
    world + extra
  }

  initialize()
}
