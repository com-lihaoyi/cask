package cask.main

import cask.model._
import cask.internal.Router.EntryPoint
import cask.internal.{DispatchTrie, Router, Util}
import io.undertow.Undertow
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.HttpString

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


  lazy val routeTries = Seq("get", "put", "post")
    .map { method =>
      method -> DispatchTrie.construct[(Routes, Routes.EndpointMetadata[_])](0,
        for ((route, metadata) <- routeList if metadata.endpoint.methods.contains(method))
        yield (Util.splitPath(metadata.endpoint.path): IndexedSeq[String], (route, metadata), metadata.endpoint.subpath)
      )
    }.toMap

  def writeResponse(exchange: HttpServerExchange, response: BaseResponse) = {
    response.headers.foreach{case (k, v) =>
      exchange.getResponseHeaders.put(new HttpString(k), v)
    }
    response.cookies.foreach(c => exchange.setResponseCookie(Cookie.toUndertow(c)))

    exchange.setStatusCode(response.statusCode)
    response.data.write(exchange.getOutputStream)
  }

  def handleError(statusCode: Int): Response = {
    Response(
      s"Error $statusCode: ${Status.codesToStatus(statusCode).reason}",
      statusCode = statusCode
    )
  }


  def defaultHandler = new HttpHandler() {
    def handleRequest(exchange: HttpServerExchange): Unit = {
      routeTries(exchange.getRequestMethod.toString.toLowerCase()).lookup(Util.splitPath(exchange.getRequestPath).toList, Map()) match{
        case None => writeResponse(exchange, handleError(404))
        case Some(((routes, metadata), bindings, remaining)) =>
          val params = for{
            endpointParams <- metadata.endpoint.getRawParams(ParamContext(exchange, remaining))
            decoratorParams <- Util.sequenceEither(
              metadata.decorators.map(e => e.getRawParams(ParamContext(exchange, remaining)))
            )
          } yield (endpointParams ++ bindings.mapValues(metadata.endpoint.wrapPathSegment)) +: decoratorParams

          val result = params match{
            case Left(resp) => resp
            case Right(paramValues) =>
              metadata.entryPoint
                .asInstanceOf[EntryPoint[cask.main.Routes, cask.model.ParamContext]]
                .invoke(routes, ParamContext(exchange, remaining), paramValues)
          }


          result match{
            case Router.Result.Success(response: BaseResponse) => writeResponse(exchange, response)
            case e: Router.Result.Error =>

              writeResponse(exchange,
                Response(
                  ErrorMsgs.formatInvokeError(
                    routes,
                    metadata.entryPoint.asInstanceOf[EntryPoint[cask.main.Routes, _]],
                    e
                  ),
                  statusCode = 500)
              )
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


