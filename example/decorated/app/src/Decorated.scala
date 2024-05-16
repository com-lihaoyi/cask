package app
object Decorated extends cask.MainRoutes {
  class User {
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

  class withCustomHeader extends cask.RawDecorator {
    def wrapFunction(request: cask.Request, delegate: Delegate) = {
      request.headers.get("x-custom-header").map(_.head) match {
        case Some(header) => delegate(Map("customHeader" -> header))
        case None =>
          cask.router.Result.Success(
            cask.model.Response(
              s"Request is missing required header: 'X-CUSTOM-HEADER'",
              400
            )
          )
      }
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

  @withCustomHeader()
  @cask.get("/echo")
  def echoHeader(request: cask.Request)(customHeader: String) = {
    customHeader
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
