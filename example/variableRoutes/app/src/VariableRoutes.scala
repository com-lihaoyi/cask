package app
object VariableRoutes extends cask.MainRoutes{
  @cask.get("/user/:userName")
  def showUserProfile(userName: String) = {
    s"User $userName"
  }

  @cask.get("/post/:postId")
  def showPost(postId: Int, param: String) = { // Mandatory query param
    s"Post $postId $param"
  }

  @cask.get("/post2/:postId") // Optional query param
  def showPostOptional(postId: Int, param: Option[String] = None) = {
    s"Post $postId $param"
  }

  @cask.get("/post3/:postId") // Optional query param with default
  def showPostDefault(postId: Int, param: String = "DEFAULT VALUE") = { 
    s"Post $postId $param"
  }

  @cask.get("/post4/:postId") // 1-or-more query param
  def showPostSeq(postId: Int, param: Seq[String]) = {
    s"Post $postId $param"
  }

  @cask.get("/post5/:postId") // 0-or-more query param
  def showPostOptionalSeq(postId: Int, param: Seq[String] = Nil) = {
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
