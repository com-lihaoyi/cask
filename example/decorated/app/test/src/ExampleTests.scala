package app
import io.undertow.Undertow

import utest._

object ExampleTests extends TestSuite {
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
    test("Decorated") - withServer(Decorated){ host =>
      requests.get(s"$host/hello/woo").text() ==> "woo31337"
      requests.get(s"$host/internal/boo").text() ==> "boo[haoyi]"
      requests.get(s"$host/internal-extra/goo").text() ==> "goo[haoyi]31337"
      requests.get(s"$host/internal-extra/goo").text() ==> "goo[haoyi]31337"
      requests.get(s"$host/hello-default?world=worldz").text() ==> "worldz[haoyi]"
      requests.get(s"$host/hello-default").text() ==> "world[haoyi]"
      requests.get(s"$host/echo", headers = Map("X-CUSTOM-HEADER" -> "header")).text() ==> "header"
    }
  }
}
