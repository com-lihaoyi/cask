package app

import java.util.concurrent.atomic.AtomicInteger

import org.asynchttpclient.ws.{WebSocket, WebSocketListener, WebSocketUpgradeHandler}
import utest._
import concurrent.ExecutionContext.Implicits.global
import cask.Logger.Console.globalLogger
object ExampleTests extends TestSuite{


  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = io.undertow.Undertow.builder
      .addHttpListener(8080, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f("http://localhost:8080")
      finally server.stop()
    res
  }

  val tests = Tests{
    test("Websockets") - withServer(Websockets4Main){ host =>
      @volatile var out = List.empty[String]
      // 4. open websocket
      val ws = cask.WsClient.connect("ws://localhost:8080/connect/haoyi"){
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
          cask.WsClient.connect("ws://localhost:8080/connect/nobody") {
            case _ => /*do nothing*/
          }
        )
        assert(ex.getMessage.contains("403"))
      }finally ws.close()
    }
  }
}
