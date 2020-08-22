package app
object ExtraParams extends cask.MainRoutes{

  @cask.get("/echo/strict")
  def echo1(param1: String) = param1

  @cask.get("/echo/lax")
  def echo2(param1: String, query: cask.Query) = query.string

  initialize()
}
