package test.cask

import io.undertow.server.HttpServerExchange

object MyServer extends cask.Routes{
  @cask.get("/user/:userName")
  def showUserProfile(userName: String) = {
    s"User $userName"
  }

  @cask.get("/post/:postId")
  def showPost(postId: Int, query: Seq[String]) = {
    s"Post $postId $query"
  }

  @cask.get("/path/::subPath")
  def showSubpath(x: HttpServerExchange, subPath: String) = {
    val length = x.getInputStream.readAllBytes().length
    println(x)
    s"Subpath $subPath + $length"
  }

  initialize()
}

object Main extends cask.Main(MyServer)
