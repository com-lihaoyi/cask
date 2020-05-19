package app

import java.util.concurrent.atomic.AtomicInteger
import castor.Context.Simple.global
import org.asynchttpclient.ws.{WebSocket, WebSocketListener, WebSocketUpgradeHandler}
import utest._

import cask.Logger.Console.globalLogger
object ExampleTests extends TestSuite{

  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = io.undertow.Undertow.builder
      .addHttpListener(8081, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f("http://localhost:8081")
      finally server.stop()
    res
  }

  val tests = Tests{
    test("Websockets") - withServer(Websockets2){ host =>
      @volatile var out = List.empty[String]
      // 4. open websocket
      val ws = cask.WsClient.connect("ws://localhost:8081/connect/haoyi"){
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
          cask.WsClient.connect("ws://localhost:8081/connect/nobody") {
            case _ => /*do nothing*/
          }
        )
        assert(ex.getMessage.contains("403"))
      }finally ws.send(cask.Ws.Close())
    }

    test("Websockets2000") - withServer(Websockets2){ host =>
      @volatile var out = List.empty[String]
      val closed = new AtomicInteger(0)
      val client = org.asynchttpclient.Dsl.asyncHttpClient();
      val ws = Seq.fill(2000)(client.prepareGet("ws://localhost:8081/connect/haoyi")
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
          new WebSocketListener() {

            override def onTextFrame(payload: String, finalFragment: Boolean, rsv: Int) = {
              ExampleTests.synchronized {
                out = payload :: out
              }
            }

            def onOpen(websocket: WebSocket) = ()

            def onClose(websocket: WebSocket, code: Int, reason: String) = {
              closed.incrementAndGet()
            }

            def onError(t: Throwable) = ()
          }).build()
        ).get())

      try{
        // 5. send messages
        ws.foreach(_.sendTextFrame("hello"))

        Thread.sleep(1500)
        out.length ==> 2000

        ws.foreach(_.sendTextFrame("world"))

        Thread.sleep(1500)
        out.length ==> 4000
        closed.get() ==> 0

        ws.foreach(_.sendTextFrame(""))

        Thread.sleep(1500)
        closed.get() ==> 2000

      }finally{
        client.close()
      }
    }

  }
}
