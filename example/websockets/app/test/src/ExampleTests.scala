package app
import com.github.andyglow.websocket.WebsocketClient
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
    'VariableRoutes - test(Websockets){ host =>
      @volatile var out = List.empty[String]
      val cli = WebsocketClient[String]("ws://localhost:8080/connect/haoyi") {
        case str => out = str :: out
      }

      // 4. open websocket
      val ws = cli.open()

      // 5. send messages
      ws ! "hello"
      ws ! "world"
      ws ! ""
      Thread.sleep(100)
      out ==> List("haoyi world", "haoyi hello")

      val cli2 = WebsocketClient[String]("ws://localhost:8080/connect/nobody") {
        case str => out = str :: out
      }

      val error =
        try cli2.open()
        catch{case e: Throwable => e.getMessage}

      assert(error.toString.contains("Invalid handshake response getStatus: 403 Forbidden"))
    }

  }
}
