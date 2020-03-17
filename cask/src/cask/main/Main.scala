package cask.main

import cask.endpoints.{WebsocketResult, WsHandler}
import cask.model._
import cask.internal.{DispatchTrie, Util}
import cask.main
import cask.router.{Decorator, EndpointMetadata, EntryPoint, RawDecorator, Result}
import cask.util.Logger
import io.undertow.Undertow
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.HttpString

/**
  * A combination of [[cask.Main]] and [[cask.Routes]], ideal for small
  * one-file web applications.
  */
class MainRoutes extends Main with Routes{
  def allRoutes = Seq(this)
}

/**
  * Defines the main entrypoint and configuration of the Cask web application.
  *
  * You can pass in an arbitrary number of [[cask.Routes]] objects for it to
  * serve, and override various properties on [[Main]] in order to configure
  * application-wide properties.
  */
abstract class Main{
  def mainDecorators: Seq[Decorator[_, _, _]] = Nil
  def allRoutes: Seq[Routes]
  def port: Int = 8080
  def host: String = "localhost"
  def debugMode: Boolean = true

  implicit def log = new cask.util.Logger.Console()

  def routeTries = Main.prepareRouteTries(allRoutes)

  def defaultHandler = new BlockingHandler(
    new Main.DefaultHandler(routeTries, mainDecorators, debugMode, handleNotFound, handleEndpointError)
  )

  def handleNotFound() = Main.defaultHandleNotFound()

  def handleEndpointError(routes: Routes,
                          metadata: EndpointMetadata[_],
                          e: cask.router.Result.Error) = {
    Main.defaultHandleError(routes, metadata, e, debugMode)
  }

  def main(args: Array[String]): Unit = {
    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(defaultHandler)
      .build
    server.start()
  }

}

object Main{
  class DefaultHandler(routeTries: Map[String, DispatchTrie[(Routes, EndpointMetadata[_])]],
                       mainDecorators: Seq[Decorator[_, _, _]],
                       debugMode: Boolean,
                       handleNotFound: () => Response.Raw,
                       handleError: (Routes, EndpointMetadata[_], Result.Error) => Response.Raw)
                      (implicit log: Logger) extends HttpHandler() {
    def handleRequest(exchange: HttpServerExchange): Unit = try {
      //        println("Handling Request: " + exchange.getRequestPath)
      val (effectiveMethod, runner) = if (exchange.getRequestHeaders.getFirst("Upgrade") == "websocket") {
        Tuple2(
          "websocket",
          (r: Any) =>
            r.asInstanceOf[WebsocketResult] match{
              case l: WsHandler =>
                io.undertow.Handlers.websocket(l).handleRequest(exchange)
              case l: WebsocketResult.Listener =>
                io.undertow.Handlers.websocket(l.value).handleRequest(exchange)
              case r: WebsocketResult.Response[Response.Data] =>
                Main.writeResponse(exchange, r.value)
            }
        )
      } else Tuple2(
        exchange.getRequestMethod.toString.toLowerCase(),
        (r: Any) => Main.writeResponse(exchange, r.asInstanceOf[Response.Raw])
      )

      routeTries(effectiveMethod).lookup(Util.splitPath(exchange.getRequestPath).toList, Map()) match {
        case None => Main.writeResponse(exchange, handleNotFound())
        case Some(((routes, metadata), routeBindings, remaining)) =>
          Decorator.invoke(
            Request(exchange, remaining),
            metadata.endpoint,
            metadata.entryPoint.asInstanceOf[EntryPoint[Routes, _]],
            routes,
            routeBindings,
            (mainDecorators ++ routes.decorators ++ metadata.decorators).toList,
            Nil
          ) match{
            case Result.Success(res) => runner(res)
            case e: Result.Error =>
              Main.writeResponse(
                exchange,
                handleError(routes, metadata, e)
              )
              None
          }
      }
      //        println("Completed Request: " + exchange.getRequestPath)
    }catch{case e: Throwable =>
      e.printStackTrace()
    }
  }

  def defaultHandleNotFound(): Response.Raw = {
    Response(
      s"Error 404: ${Status.codesToStatus(404).reason}",
      statusCode = 404
    )
  }

  def prepareRouteTries(allRoutes: Seq[Routes]): Map[String, DispatchTrie[(Routes, EndpointMetadata[_])]] = {
    val routeList = for{
      routes <- allRoutes
      route <- routes.caskMetadata.value.map(x => x: EndpointMetadata[_])
    } yield (routes, route)

    val allMethods: Set[String] =
      routeList.flatMap(_._2.endpoint.methods).map(_.toLowerCase).toSet

    allMethods
      .map { method =>
        method -> DispatchTrie.construct[(Routes, EndpointMetadata[_])](0,
          for ((route, metadata) <- routeList if metadata.endpoint.methods.contains(method))
            yield (Util.splitPath(metadata.endpoint.path): collection.IndexedSeq[String], (route, metadata), metadata.endpoint.subpath)
        )
      }.toMap
  }
  def writeResponse(exchange: HttpServerExchange, response: Response.Raw) = {
    response.headers.foreach{case (k, v) =>
      exchange.getResponseHeaders.put(new HttpString(k), v)
    }
    response.cookies.foreach(c => exchange.setResponseCookie(Cookie.toUndertow(c)))

    exchange.setStatusCode(response.statusCode)
    response.data.write(exchange.getOutputStream)
  }

  def defaultHandleError(routes: Routes,
                         metadata: EndpointMetadata[_],
                         e: Result.Error,
                         debugMode: Boolean)
                        (implicit log: Logger) = {
    e match {
      case e: Result.Error.Exception => log.exception(e.t)
      case _ => // do nothing
    }
    val statusCode = e match {
      case _: Result.Error.Exception => 500
      case _: Result.Error.InvalidArguments => 400
      case _: Result.Error.MismatchedArguments => 400
    }

    val str =
      if (!debugMode) s"Error $statusCode: ${Status.codesToStatus(statusCode).reason}"
      else ErrorMsgs.formatInvokeError(
        routes,
        metadata.entryPoint.asInstanceOf[EntryPoint[cask.main.Routes, _]],
        e
      )

    Response(str, statusCode = statusCode)

  }

}

