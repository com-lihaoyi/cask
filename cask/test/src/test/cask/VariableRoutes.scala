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

  @cask.get("/path/::subPath")
  def showSubpath(subPath: String) = {
    s"Subpath $subPath"
  }

  initialize()
}
