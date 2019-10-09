package app
object Decorated2 extends cask.MainRoutes{
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

  override def decorators = Seq(new withExtra())

  @cask.get("/hello/:world")
  def hello(world: String)(extra: Int) = {
    world + extra
  }

  @loggedIn()
  @cask.get("/internal-extra/:world")
  def internalExtra(world: String)(user: User)(extra: Int) = {
    world + user + extra
  }

  @loggedIn()
  @cask.get("/ignore-extra/:world")
  def ignoreExtra(world: String)(user: User)(extra: Int)  = {
    world + user
  }

  initialize()
}
