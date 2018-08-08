package test.cask

import cask.internal.Router
import cask.model.{ParamContext, Response}

object Decorated extends cask.MainRoutes{
  class User{
    override def toString = "[haoyi]"
  }
  class loggedIn extends cask.Decorator {
    def wrapMethodOutput(ctx: ParamContext,
                         delegate: Map[String, Input] => Router.Result[Output]): Router.Result[Response] = {
      delegate(Map("user" -> new User()))
    }
  }
  class withExtra extends cask.Decorator {
    def wrapMethodOutput(ctx: ParamContext,
                         delegate: Map[String, Input] => Router.Result[Output]): Router.Result[Response] = {
      delegate(Map("extra" -> 31337))
    }
  }

  @withExtra()
  @cask.get("/hello/:world")
  def hello(world: String)(extra: Int) = {
    world + extra
  }

  @loggedIn()
  @cask.get("/internal/:world")
  def internal(world: String)(user: User) = {
    world + user
  }

  @withExtra()
  @loggedIn()
  @cask.get("/internal-extra/:world")
  def internalExtra(world: String)(user: User)(extra: Int) = {
    world + user + extra
  }

  @withExtra()
  @loggedIn()
  @cask.get("/ignore-extra/:world")
  def ignoreExtra(world: String)(user: User) = {
    world + user
  }

  initialize()
}
