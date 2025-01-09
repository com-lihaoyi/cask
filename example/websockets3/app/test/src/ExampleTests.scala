package app

import utest._
import cask.Logger.Console.globalLogger
import castor.Context.Simple.global

object ExampleTests extends TestSuite{


  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val port = cask.util.SocketUtils.getFreeTcpPort
    val server = io.undertow.Undertow.builder
      .addHttpListener(port, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f(s"ws://localhost:$port")
      finally server.stop()
    res
  }

  val tests = Tests{
    test("Websockets") - withServer(Websockets3Main){ host =>
      @volatile var out = List.empty[String]
      // 4. open websocket
      val ws = cask.WsClient.connect(s"$host/connect/haoyi"){
        case cask.Ws.Text(s) => out = s :: out
      }

      try {
        // 5. send messages
        ws.send(cask.Ws.Text("hello"))
        ws.send(cask.Ws.Text("world"))
        ws.send(cask.Ws.Text(""))
        Thread.sleep(100)
        out ==> List("haoyi world", "haoyi hello")

        val ex = intercept[Exception](
          cask.WsClient.connect(s"$host/connect/nobody") {
            case _ => /*do nothing*/
          }
        )
        assert(ex.getMessage.contains("403"))
      }finally ws.send(cask.Ws.Close())
    }
  }
}
