package app

case class Websockets4()(implicit cc: castor.Context,
                         log: cask.Logger) extends cask.Routes{
  // make sure compress decorator passes non-requests through correctly
  override def decorators = Seq(new cask.decorators.compress())
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

object Websockets4Main extends cask.Main{
  val allRoutes = Seq(Websockets4())
}
