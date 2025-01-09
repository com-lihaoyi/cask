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

  val tests = Tests {
    test("Scalatags") - withServer(Scalatags) { host =>
      val body = requests.get(host).text()

      assert(
        body.contains("<h1>Hello World</h1>"),
        body.contains("<p>I am cow</p>"),
      )
    }
  }
}
