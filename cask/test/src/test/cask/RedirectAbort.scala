package test.cask

object RedirectAbort extends cask.MainRoutes{
  @cask.get("/")
  def index() = {
    cask.redirect("/login")
  }

  @cask.get("/login")
  def login() = {
    cask.abort(401)
  }

  initialize()
}

