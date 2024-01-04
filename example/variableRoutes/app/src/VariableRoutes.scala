package app
object VariableRoutes extends cask.MainRoutes{
  @cask.get("/user/:userName") // variable path segment, e.g. HOST/user/lihaoyi
  def getUserProfile(userName: String) = {
    s"User $userName"
  }

  @cask.get("/path") // GET allowing arbitrary sub-paths, e.g. HOST/path/foo/bar/baz
  def getSubpath(remainingPathSegments: cask.RemainingPathSegments) = {
    s"Subpath ${remainingPathSegments.value}"
  }

  @cask.post("/path") // POST allowing arbitrary sub-paths, e.g. HOST/path/foo/bar/baz
  def postArticleSubpath(remainingPathSegments: cask.RemainingPathSegments) = {
    s"POST Subpath ${remainingPathSegments.value}"
  }

  initialize()
}
