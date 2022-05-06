package app
object VariableRoutes extends cask.MainRoutes{
  @cask.get("/user/:userName")
  def showUserProfile(userName: String) = {
    s"User $userName"
  }

  @cask.get("/post/:postId")
  def showPost(postId: Int, param: Seq[String]) = {
    s"Post $postId $param"
  }

  @cask.get("/path", subpath = true)
  def showSubpath(request: cask.Request) = {
    s"Subpath ${request.remainingPathSegments}"
  }

  @cask.post("/path", subpath = true)
  def postShowSubpath(request: cask.Request) = {
    s"POST Subpath ${request.remainingPathSegments}"
  }

  initialize()
}
