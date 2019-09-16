package app

case class Websockets3()(implicit val log: cask.Logger) extends cask.Routes{
  @cask.websocket("/connect/:userName")
  def showUserProfile(userName: String): cask.WebsocketResult = {
    if (userName != "haoyi") cask.Response("", statusCode = 403)
    else cask.WsHandler { channel =>
      cask.WsActor {
        case cask.WsActor.Text("") => channel.send(cask.WsActor.Close())
        case cask.WsActor.Text(data) =>
          channel.send(cask.WsActor.Text(userName + " " + data))
      }
    }
  }

  initialize()
}

object Websockets3Main extends cask.Main{
  val allRoutes = Seq(Websockets3())
}
