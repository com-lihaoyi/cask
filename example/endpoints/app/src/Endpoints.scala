package app

class custom(val path: String, val methods: Seq[String]) extends cask.Endpoint[Int]{
  type Output = Int
  def wrapFunction(ctx: cask.Request, delegate: Delegate): OuterReturned = {
    delegate(Map()).map{num =>
      cask.Response("Echo " + num, statusCode = num)
    }
  }

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
