package test.cask

object MinimalApplication extends cask.MainRoutes{
  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  @cask.get("/request-info")
  def hello(request: cask.Request) = {
    request.queryParams.toString + "\n" +
    request.headers.toString + "\n" +
    request.cookies.toString
  }

  initialize()
}
