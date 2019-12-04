package app
object Decorated extends cask.MainRoutes{
  class User{
    override def toString = "[haoyi]"
  }
  class loggedIn extends cask.RawDecorator {
    def wrapFunction(ctx: cask.Request, delegate: Delegate) = {
      delegate(Map("user" -> new User()))
    }
  }
  class withExtra extends cask.RawDecorator {
    def wrapFunction(ctx: cask.Request, delegate: Delegate) = {
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

  @loggedIn()
  @cask.get("/hello-default")
  def defaults(world: String = "world")(user: User) = {
    world + user
  }
  initialize()
}
