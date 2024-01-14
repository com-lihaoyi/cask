package app
object VariableRoutes extends cask.MainRoutes{
  @cask.get("/user/:userName") // variable path segment, e.g. HOST/user/lihaoyi
  def getUserProfile(userName: String) = {
    s"User $userName"
  }

  @cask.get("/path") // GET allowing arbitrary sub-paths, e.g. HOST/path/foo/bar/baz
  def getSubpath(segments: cask.RemainingPathSegments) = {
    s"Subpath ${segments.value}"
  }

  @cask.post("/path") // POST allowing arbitrary sub-paths, e.g. HOST/path/foo/bar/baz
  def postArticleSubpath(segments: cask.RemainingPathSegments) = {
    s"POST Subpath ${segments.value}"
  }

  initialize()
}
