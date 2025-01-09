package app
import io.undertow.Undertow
import utest._

object ExampleTests extends TestSuite{
  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val port = cask.util.SocketUtils.getFreeTcpPort
    val server = Undertow.builder
      .addHttpListener(port, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f(s"http://localhost:$port")
      finally server.stop()
    res
  }

  val tests = Tests{
    test("Endpoints") - withServer(Endpoints){ host =>
      requests.get(s"$host/echo/200").text() ==> "Echo 200"
      requests.get(s"$host/echo/200").statusCode ==> 200
      requests.get(s"$host/echo/400", check = false).text() ==> "Echo 400"
      requests.get(s"$host/echo/400", check = false).statusCode ==> 400
    }
  }
}
