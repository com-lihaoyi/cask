package app
object RedirectAbort extends cask.MainRoutes{
  @cask.get("/")
  def index() = {
    cask.Redirect("/login")
  }

  @cask.get("/login")
  def login() = {
    cask.Abort(401)
  }

  initialize()
}
