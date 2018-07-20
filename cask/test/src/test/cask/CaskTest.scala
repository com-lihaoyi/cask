package test.cask


object MyServer extends cask.Server(){

  def x = "/ext"
  @cask.route("/user/:username" + (x * 2))
  def showUserProfile(userName: String) = {
    //  show the user profile for that user
    s"User $userName"
  }

  @cask.route("/post/:int")
  def showPost(postId: Int) = {
    // show the post with the given id, the id is an integer
    s"Post $postId"
  }

  @cask.route("/path/:subPath")
  def show_subpath(subPath: String) = {
    // show the subpath after /path/
    s"Subpath $subPath"
  }

  initialize()
  println(routes.value)
}

object Main extends cask.Main(MyServer){
  def main(args: Array[String]): Unit = {
    MyServer
  }
}

