package app

import org.asynchttpclient.ws.{WebSocket, WebSocketListener, WebSocketUpgradeHandler}
import utest._

object ExampleTests extends TestSuite{


  def test[T](example: cask.main.BaseMain)(f: String => T): T = {
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
    'Websockets - test(Websockets){ host =>
      @volatile var out = List.empty[String]
      val client = org.asynchttpclient.Dsl.asyncHttpClient();
      try{

        // 4. open websocket
        val ws: WebSocket = client.prepareGet("ws://localhost:8080/connect/haoyi")
          .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketListener() {

              override def onTextFrame(payload: String, finalFragment: Boolean, rsv: Int) {
                out = payload :: out
              }

              def onOpen(websocket: WebSocket) = ()

              def onClose(websocket: WebSocket, code: Int, reason: String) = ()

              def onError(t: Throwable) = ()
            }).build()
          ).get()

        // 5. send messages
        ws.sendTextFrame("hello")
        ws.sendTextFrame("world")
        ws.sendTextFrame("")
        Thread.sleep(100)
        out ==> List("haoyi world", "haoyi hello")

        var error: String = ""
        val cli2 = client.prepareGet("ws://localhost:8080/connect/nobody")
          .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketListener() {

              def onOpen(websocket: WebSocket) = ()

              def onClose(websocket: WebSocket, code: Int, reason: String) = ()

              def onError(t: Throwable) = {
                error = t.toString
              }
            }).build()
          ).get()

        assert(error.contains("403"))

      } finally{
        client.close()
      }
    }

    'Websockets1000 - test(Websockets){ host =>
      @volatile var out = List.empty[String]
      val client = org.asynchttpclient.Dsl.asyncHttpClient();
      val ws = Seq.fill(1000)(client.prepareGet("ws://localhost:8080/connect/haoyi")
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
          new WebSocketListener() {

            override def onTextFrame(payload: String, finalFragment: Boolean, rsv: Int) {
              out = payload :: out
            }

            def onOpen(websocket: WebSocket) = ()

            def onClose(websocket: WebSocket, code: Int, reason: String) = ()

            def onError(t: Throwable) = ()
          }).build()
        ).get())

      try{
        // 4. open websocket

        // 5. send messages
        ws.foreach{ w =>
          w.sendTextFrame("hello")
          Thread.sleep(1)
        }
        ws.foreach { w =>
          w.sendTextFrame("world")
          Thread.sleep(1)
        }
        ws.foreach { w =>
          w.sendTextFrame("")
          Thread.sleep(1)
        }
        Thread.sleep(1500)
        out.length ==> 2000

      }finally{
        client.close()
      }
    }

  }
}
