package app


class custom(val path: String, val methods: Seq[String]) extends cask.Endpoint{
  type Output = Int
  def wrapFunction(ctx: cask.ParamContext, delegate: Delegate): Returned = {
    delegate(Map()) match{
      case cask.internal.Router.Result.Success(num) =>
        cask.internal.Router.Result.Success(
          cask.Response("Echo " + num, statusCode = num)
        )
      case e: cask.internal.Router.Result.Error => e
    }
  }

  // Change this if you want to change
  def wrapPathSegment(s: String) = Seq(s)

  type Input = Seq[String]
  type InputParser[T] = cask.endpoints.QueryParamReader[T]
}

object Endpoints extends cask.MainRoutes{


  @custom("/echo/:status", methods = Seq("get"))
  def echoStatus(status: String) = {
    status.toInt
  }

  initialize()
}
