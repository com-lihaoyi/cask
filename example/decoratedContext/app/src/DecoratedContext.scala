package app

case class Context(
  session: Session
)

case class Session(data: collection.mutable.Map[String, String])

trait CustomParser[T] extends cask.router.ArgReader[Any, T, Context]
object CustomParser:
  given CustomParser[Context] with
    def arity = 0
    def read(ctx: Context, label: String, input: Any): Context = ctx
  given CustomParser[Session] with
    def arity = 0
    def read(ctx: Context, label: String, input: Any): Session = ctx.session
  given literal[Literal]: CustomParser[Literal] with
    def arity = 1
    def read(ctx: Context, label: String, input: Any): Literal = input.asInstanceOf[Literal]

object DecoratedContext extends cask.MainRoutes{

  class custom extends cask.router.Decorator[cask.Response.Raw, cask.Response.Raw, Any, Context]{

    override type InputParser[T] = CustomParser[T]

    def wrapFunction(req: cask.Request, delegate: Delegate) = {
      // Create a custom context out of the request. Custom contexts are useful
      // to group an expensive operation that may be used by multiple
      // parameter readers.
      val ctx = Context(Session(collection.mutable.Map.empty)) // this would typically be populated from a signed cookie

      delegate(ctx, Map("user" -> 1337)).map{ response =>
        val extraCookies = ctx.session.data.map(
          (k, v) => cask.Cookie(k, v)
        )

        response.copy(
          cookies = response.cookies ++ extraCookies
        )
      }

    }
  }

  @custom()
  @cask.get("/hello/:world")
  def hello(world: String, req: cask.Request)(session: Session, user: Int) = {
    session.data("hello") = "world"
    world + user
  }

  initialize()
}
