package cask.main

import cask.endpoints.WebsocketResult
import cask.model._
import cask.internal.Router.EntryPoint
import cask.internal.{DispatchTrie, Router, Util}
import io.undertow.Undertow
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.HttpString

/**
  * A combination of [[cask.Main]] and [[cask.Routes]], ideal for small
  * one-file web applications.
  */
class MainRoutes extends BaseMain with Routes{
  def allRoutes = Seq(this)
}

/**
  * Defines the main entrypoint and configuration of the Cask web application.
  *
  * You can pass in an arbitrary number of [[cask.Routes]] objects for it to
  * serve, and override various properties on [[Main]] in order to configure
  * application-wide properties.
  */
class Main(servers0: Routes*) extends BaseMain{
  def allRoutes = servers0.toSeq
}
abstract class BaseMain{
  def mainDecorators = Seq.empty[cask.main.Decorator]
  def allRoutes: Seq[Routes]
  def port: Int = 8080
  def host: String = "localhost"
  def debugMode: Boolean = true

  lazy val routeList = for{
    routes <- allRoutes
    route <- routes.caskMetadata.value.map(x => x: Routes.EndpointMetadata[_])
  } yield (routes, route)


  lazy val routeTries = Seq("get", "put", "post", "websocket")
    .map { method =>
      method -> DispatchTrie.construct[(Routes, Routes.EndpointMetadata[_])](0,
        for ((route, metadata) <- routeList if metadata.endpoint.methods.contains(method))
        yield (Util.splitPath(metadata.endpoint.path): IndexedSeq[String], (route, metadata), metadata.endpoint.subpath)
      )
    }.toMap

  def writeResponse(exchange: HttpServerExchange, response: Response) = {
    response.headers.foreach{case (k, v) =>
      exchange.getResponseHeaders.put(new HttpString(k), v)
    }
    response.cookies.foreach(c => exchange.setResponseCookie(Cookie.toUndertow(c)))

    exchange.setStatusCode(response.statusCode)
    response.data.write(exchange.getOutputStream)
  }

  def handleNotFound(): Response = {
    Response(
      s"Error 404: ${Status.codesToStatus(404).reason}",
      statusCode = 404
    )
  }


  def defaultHandler = new BlockingHandler(
    new HttpHandler() {
      def handleRequest(exchange: HttpServerExchange): Unit = {
        val (effectiveMethod, runner) = if (exchange.getRequestHeaders.getFirst("Upgrade") == "websocket") {
          "websocket" -> ((r: Any) =>
            r.asInstanceOf[WebsocketResult] match{
              case l: WebsocketResult.Listener =>
                io.undertow.Handlers.websocket(l.value).handleRequest(exchange)
              case r: WebsocketResult.Response =>
                writeResponseHandler(r).handleRequest(exchange)
            }
            )
        } else (
          exchange.getRequestMethod.toString.toLowerCase(),
          (r: Any) => writeResponse(exchange, r.asInstanceOf[Response])
        )

        routeTries(effectiveMethod).lookup(Util.splitPath(exchange.getRequestPath).toList, Map()) match {
          case None => writeResponse(exchange, handleNotFound())
          case Some(((routes, metadata), routeBindings, remaining)) =>
            val ctx = Request(exchange, remaining)
            def rec(remaining: List[Decorator],
                    bindings: List[Map[String, Any]]): Router.Result[Any] = try {
              remaining match {
                case head :: rest =>
                  head.wrapFunction(
                    ctx,
                    args => rec(rest, args :: bindings).asInstanceOf[Router.Result[head.Output]]
                  )

                case Nil =>
                  metadata.endpoint.wrapFunction(ctx, endpointBindings =>
                    metadata.entryPoint
                      .asInstanceOf[EntryPoint[cask.main.Routes, cask.model.Request]]
                      .invoke(
                        routes, ctx,
                        (endpointBindings ++ routeBindings.mapValues(metadata.endpoint.wrapPathSegment))
                        :: bindings.reverse
                      )
                      .asInstanceOf[Router.Result[Nothing]]
                  )
              }
              // Make sure we wrap any exceptions that bubble up from decorator
              // bodies, so outer decorators do not need to worry about their
              // delegate throwing on them
            }catch{case e: Throwable => Router.Result.Error.Exception(e) }

            rec((metadata.decorators ++ routes.decorators ++ mainDecorators).toList, Nil)match{
              case Router.Result.Success(res) => runner(res)
              case e: Router.Result.Error =>
                writeResponse(exchange, handleEndpointError(exchange, routes, metadata, e))
                None
            }
        }

      }
    }
  )

  def writeResponseHandler(r: WebsocketResult.Response) = new BlockingHandler(
    new HttpHandler {
      def handleRequest(exchange: HttpServerExchange): Unit = {
        writeResponse(exchange, r.value)
      }
    }
  )

  def handleEndpointError(exchange: HttpServerExchange,
                          routes: Routes,
                          metadata: Routes.EndpointMetadata[_],
                          e: Router.Result.Error) = {
    val statusCode = e match {
      case _: Router.Result.Error.Exception => 500
      case _: Router.Result.Error.InvalidArguments => 400
      case _: Router.Result.Error.MismatchedArguments => 400
    }
    Response(
      if (!debugMode) s"Error $statusCode: ${Status.codesToStatus(statusCode).reason}"
      else ErrorMsgs.formatInvokeError(
        routes,
        metadata.entryPoint.asInstanceOf[EntryPoint[cask.main.Routes, _]],
        e
      ),
      statusCode = statusCode
    )

  }


  def main(args: Array[String]): Unit = {
    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(defaultHandler)
      .build
    server.start()
  }
}


