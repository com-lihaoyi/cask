package test.cask

object VariableRoutes extends cask.MainRoutes{
  @cask.get("/user/:userName")
  def showUserProfile(userName: String) = {
    s"User $userName"
  }

  @cask.get("/post/:postId")
  def showPost(postId: Int, query: Seq[String]) = {
    s"Post $postId $query"
  }

  @cask.get("/path", subpath = true)
  def showSubpath(subPath: cask.Subpath) = {
    s"Subpath ${subPath.value}"
  }

  initialize()
}
