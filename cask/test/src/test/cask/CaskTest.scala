package test.cask

import io.undertow.io.Receiver.{ErrorCallback, FullBytesCallback}
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
    x.getRequestReceiver().receiveFullBytes((exchange, data) => {

    }: FullBytesCallback,
      (exchange, exception) => {

      }: ErrorCallback
    )
    println(x)
    s"Subpath $subPath"
  }

//  @cask.post("/echo-size")
//  def echoSize(x: HttpServerExchange, subPath: String) = {
//    println(x)
//    s"Subpath $subPath"
//  }

  initialize()
}

object Main extends cask.Main(MyServer)
