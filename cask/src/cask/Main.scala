package cask
import cask.Router.EntryPoint
import java.io.OutputStream
import java.nio.ByteBuffer

import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, HttpString}

class MainRoutes extends BaseMain with Routes{
  def servers = Seq(this)
}
class Main(servers0: Routes*) extends BaseMain{
  def servers = servers0.toSeq
}
abstract class BaseMain{
  def servers: Seq[Routes]
  val port: Int = 8080
  val host: String = "localhost"

  val allRoutes = for{
    server <- servers
    route <- server.caskMetadata.value.map(x => x: Routes.RouteMetadata[_])
  } yield (server, route)

  val defaultHandler = new HttpHandler() {
    def handleRequest(exchange: HttpServerExchange): Unit = {
      val routeOpt =
        allRoutes
          .iterator
          .map { case (s: Routes, r: Routes.RouteMetadata[_]) =>
            Util.matchRoute(r.metadata.path, exchange.getRequestPath).map((s, r, _))
          }
          .flatten
          .toStream
          .headOption


      routeOpt match{
        case None =>

          exchange.setStatusCode(404)
          exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, "text/plain")
          exchange.getResponseSender.send("404 Not Found")
        case Some((server, route, bindings)) =>
          import collection.JavaConverters._
          val allBindings =
            bindings.toSeq ++
              exchange.getQueryParameters
                .asScala
                .toSeq
                .flatMap{case (k, vs) => vs.asScala.map((k, _))}

          val result = route.entryPoint
            .asInstanceOf[EntryPoint[server.type, HttpServerExchange]]
            .invoke(server, exchange, allBindings.map{case (k, v) => (k, Some(v))})

          result match{
            case Router.Result.Success(response: Response) =>
              response.headers.foreach{case (k, v) =>
                exchange.getResponseHeaders.put(new HttpString(k), v)
              }

              exchange.setStatusCode(response.statusCode)


              response.data.write(
                new OutputStream {
                  def write(b: Int) = {
                    exchange.getResponseSender.send(ByteBuffer.wrap(Array(b.toByte)))
                  }
                  override def write(b: Array[Byte]) = {
                    exchange.getResponseSender.send(ByteBuffer.wrap(b))
                  }
                  override def write(b: Array[Byte], off: Int, len: Int) = {
                    exchange.getResponseSender.send(ByteBuffer.wrap(b.slice(off, off + len)))
                  }
                }
              )
            case err: Router.Result.Error =>
              exchange.setStatusCode(400)
              exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, "text/plain")
              exchange.getResponseSender.send("400 Not Found " + result)
          }


      }
    }
  }

  def main(args: Array[String]): Unit = {
    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(new BlockingHandler(defaultHandler))
      .build
    server.start()
  }
}


