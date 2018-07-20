package test.cask

object MyServer extends cask.Routes{
  @cask.get("/user/:userName")
  def showUserProfile(userName: String) = {
    s"User $userName"
  }

  @cask.post("/post/:postId")
  def showPost(postId: Int, query: String) = {
    s"Post $postId $query"
  }

  @cask.put("/path/::subPath")
  def showSubpath(subPath: String) = {
    s"Subpath $subPath"
  }

  initialize()
}

object Main extends cask.Main(MyServer)
