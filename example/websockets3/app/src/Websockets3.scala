package app

case class Websockets3()(implicit val actorContext: castor.Context,
                         val log: cask.Logger) extends cask.Routes{
  @cask.websocket("/connect/:userName")
  def showUserProfile(userName: String): cask.WebsocketResult = {
    if (userName != "haoyi") cask.Response("", statusCode = 403)
    else cask.WsHandler { channel =>
      cask.WsActor {
        case cask.Ws.Text("") => channel.send(cask.Ws.Close())
        case cask.Ws.Text(data) =>
          channel.send(cask.Ws.Text(userName + " " + data))
      }
    }
  }

  initialize()
}

object Websockets3Main extends cask.Main{
  val allRoutes = Seq(Websockets3())
}
