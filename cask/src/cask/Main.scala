package cask
import cask.Router.EntryPoint
import java.io.OutputStream
import java.nio.ByteBuffer

import cask.Routes.RoutesEndpointsMetadata
import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, HttpString}

class MainRoutes extends BaseMain with Routes{
  def allRoutes = Seq(this)
}
class Main(servers0: Routes*) extends BaseMain{
  def allRoutes = servers0.toSeq
}
abstract class BaseMain{
  def allRoutes: Seq[Routes]
  val port: Int = 8080
  val host: String = "localhost"

  lazy val routeList = for{
    routes <- allRoutes
    route <- routes.caskMetadata.value.map(x => x: Routes.EndpointMetadata[_])
  } yield (routes, route)

  lazy val routeTrie = DispatchTrie.construct[(Routes, Routes.EndpointMetadata[_])](0,
    for((route, metadata) <- routeList)
    yield (Util.splitPath(metadata.metadata.path): IndexedSeq[String], (route, metadata), metadata.metadata.subpath)
  )

  def handleError(statusCode: Int): Response = {
    Response(
      s"Error $statusCode: ${Status.codesToStatus(statusCode).reason}",
      statusCode = statusCode
    )
  }

  def writeResponse(exchange: HttpServerExchange, response: Response) = {
    response.headers.foreach{case (k, v) =>
      exchange.getResponseHeaders.put(new HttpString(k), v)
    }
    response.cookies.foreach(c => exchange.setResponseCookie(Cookie.toUndertow(c)))

    exchange.setStatusCode(response.statusCode)
    response.data.write(exchange.getOutputStream)
  }

  lazy val defaultHandler = new HttpHandler() {
    def handleRequest(exchange: HttpServerExchange): Unit = {
      routeTrie.lookup(Util.splitPath(exchange.getRequestPath).toList, Map()) match{
        case None => writeResponse(exchange, handleError(404))
        case Some(((routes, metadata), bindings, remaining)) =>
          val result = metadata.metadata.handle(
            exchange, remaining, bindings, routes,
            metadata.entryPoint.asInstanceOf[EntryPoint[Routes, (HttpServerExchange, Seq[String])]]
          )

          result match{
            case Router.Result.Success(response: Response) => writeResponse(exchange, response)
            case Router.Result.Error.Exception(e) =>
              println(e)
              e.printStackTrace()
              writeResponse(exchange, handleError(500))
            case err: Router.Result.Error =>
              println(err)
              writeResponse(exchange, handleError(400))
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


