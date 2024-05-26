package cask.main

import cask.endpoints.{WebsocketResult, WsHandler}
import cask.model._
import cask.internal.{DispatchTrie, Util, ThreadBlockingHandler}
import Response.Raw
import cask.router.{Decorator, EndpointMetadata, EntryPoint, Result}
import cask.util.Logger
import io.undertow.Undertow
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.HttpString

import java.util.concurrent.Executor
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
 * A combination of [[cask.Main]] and [[cask.Routes]], ideal for small
 * one-file web applications.
 */
class MainRoutes extends Main with Routes {
  def allRoutes: Seq[Routes] = Seq(this)
}

/**
 * Defines the main entrypoint and configuration of the Cask web application.
 *
 * You can pass in an arbitrary number of [[cask.Routes]] objects for it to
 * serve, and override various properties on [[Main]] in order to configure
 * application-wide properties.
 *
 * By default,the [[Routes]] running inside a worker threads, and can run with
 * `Virtual Threads` if the runtime supports it and the property `cask.virtualThread.enabled` is set to `true`.
 */
abstract class Main {
  def mainDecorators: Seq[Decorator[_, _, _]] = Nil

  def allRoutes: Seq[Routes]

  def port: Int = 8080

  def host: String = "localhost"

  def verbose = false

  def debugMode: Boolean = true

  def createExecutionContext: ExecutionContext = castor.Context.Simple.executionContext

  def createActorContext = new castor.Context.Simple(executionContext, log.exception)

  val executionContext: ExecutionContext = createExecutionContext
  implicit val actorContext: castor.Context = createActorContext

  implicit def log: cask.util.Logger = new cask.util.Logger.Console()

  def dispatchTrie: DispatchTrie[Map[String, (Routes, EndpointMetadata[_])]] = Main.prepareDispatchTrie(allRoutes)

  protected def handlerExecutor(): Executor = {
    null
  }

  def defaultHandler: HttpHandler = {
    val mainHandler = new Main.DefaultHandler(dispatchTrie, mainDecorators, debugMode, handleNotFound, handleMethodNotAllowed, handleEndpointError)
    val executor = handlerExecutor()
    if (handlerExecutor ne null) {
      new ThreadBlockingHandler(executor, mainHandler)
    } else new BlockingHandler(mainHandler)
  }

  def handler(): HttpHandler = defaultHandler

  def handleNotFound(): Raw = Main.defaultHandleNotFound()

  def handleMethodNotAllowed(): Raw = Main.defaultHandleMethodNotAllowed()

  def handleEndpointError(routes: Routes,
                          metadata: EndpointMetadata[_],
                          e: cask.router.Result.Error): Response[String] = {
    Main.defaultHandleError(routes, metadata, e, debugMode)
  }

  def main(@nowarn args: Array[String]): Unit = {
    if (!verbose) Main.silenceJboss()
    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(defaultHandler)
      .build
    server.start()
  }

}

object Main {
  class DefaultHandler(dispatchTrie: DispatchTrie[Map[String, (Routes, EndpointMetadata[_])]],
                       mainDecorators: Seq[Decorator[_, _, _]],
                       @nowarn debugMode: Boolean,
                       handleNotFound: () => Response.Raw,
                       handleMethodNotAllowed: () => Response.Raw,
                       handleError: (Routes, EndpointMetadata[_], Result.Error) => Response.Raw)
                      (implicit log: Logger) extends HttpHandler() {
    def handleRequest(exchange: HttpServerExchange): Unit = try {
      //        println("Handling Request: " + exchange.getRequestPath)
      val (effectiveMethod, runner) = if ("websocket".equalsIgnoreCase(exchange.getRequestHeaders.getFirst("Upgrade"))) {
        Tuple2(
          "websocket",
          (r: Any) =>
            r.asInstanceOf[WebsocketResult] match {
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

      val decodedSegments = Util
        .splitPath(exchange.getRequestURI)
        .iterator
        .map(java.net.URLDecoder.decode(_, "UTF-8"))
        .toList

      dispatchTrie.lookup(decodedSegments, Map()) match {
        case None => Main.writeResponse(exchange, handleNotFound())
        case Some((methodMap, routeBindings, remaining)) =>
          methodMap.get(effectiveMethod) match {
            case None => Main.writeResponse(exchange, handleMethodNotAllowed())
            case Some((routes, metadata)) =>
              Decorator.invoke(
                Request(exchange, remaining),
                metadata.endpoint,
                metadata.entryPoint.asInstanceOf[EntryPoint[Routes, _]],
                routes,
                routeBindings,
                (mainDecorators ++ routes.decorators ++ metadata.decorators).toList,
                Nil
              ) match {
                case Result.Success(res) => runner(res)
                case e: Result.Error =>
                  Main.writeResponse(
                    exchange,
                    handleError(routes, metadata, e)
                  )
              }
          }
      }
    } catch {
      case NonFatal(e) =>
        log.exception(e)
    }
  }

  def defaultHandleNotFound(): Response.Raw = {
    Response(
      s"Error 404: ${Status.codesToStatus(404).reason}",
      statusCode = 404
    )
  }

  def defaultHandleMethodNotAllowed(): Response.Raw = {
    Response(
      s"Error 405: ${Status.codesToStatus(405).reason}",
      statusCode = 405
    )
  }

  def prepareDispatchTrie(allRoutes: Seq[Routes]): DispatchTrie[Map[String, (Routes, EndpointMetadata[_])]] = {
    val flattenedRoutes = for {
      routes <- allRoutes
      metadata <- routes.caskMetadata.value
    } yield {
      val segments = Util.splitPath(metadata.endpoint.path)
      val methods = metadata.endpoint.methods.map(_ -> (routes, metadata: EndpointMetadata[_]))
      val methodMap = methods.toMap[String, (Routes, EndpointMetadata[_])]
      val subpath =
        metadata.endpoint.subpath ||
          metadata.entryPoint.argSignatures.exists(_.exists(_.reads.remainingPathSegments))

      (segments, methodMap, subpath)
    }

    val dispatchInputs = flattenedRoutes.groupBy(_._1).map { case (segments, values) =>
      val methodMap = values.flatMap(_._2)
      val hasSubPath = values.exists(_._3)
      (segments, methodMap, hasSubPath)
    }.toSeq

    DispatchTrie.construct(0, dispatchInputs)(_.map(_._1)).map(_.toMap)
  }

  def writeResponse(exchange: HttpServerExchange, response: Response.Raw): Unit = {
    response.data.headers.foreach { case (k, v) =>
      exchange.getResponseHeaders.put(new HttpString(k), v)
    }
    response.headers.foreach { case (k, v) =>
      exchange.getResponseHeaders.put(new HttpString(k), v)
    }
    response.cookies.foreach(c => exchange.setResponseCookie(Cookie.toUndertow(c)))

    exchange.setStatusCode(response.statusCode)
    val output = exchange.getOutputStream
    response.data.write(new java.io.OutputStream {
      def write(b: Int): Unit = output.write(b)

      override def write(b: Array[Byte]): Unit = output.write(b)

      override def write(b: Array[Byte], off: Int, len: Int): Unit = output.write(b, off, len)

      override def close(): Unit = {
        if (!exchange.isComplete) output.close()
      }

      override def flush(): Unit = {
        if (!exchange.isComplete) output.flush()
      }
    })
  }

  def defaultHandleError(routes: Routes,
                         metadata: EndpointMetadata[_],
                         e: Result.Error,
                         debugMode: Boolean)
                        (implicit log: Logger): Response[String] = {
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

  def silenceJboss(): Unit = {
    // Some jboss classes litter logs from their static initializers. This is a
    // workaround to stop this rather annoying behavior.
    val tmp = System.out
    System.setOut(null)
    org.jboss.threads.Version.getVersionString() // this causes the static initializer to be run
    System.setOut(tmp)

    // Other loggers print way too much information. Set them to only print
    // interesting stuff.
    val level = java.util.logging.Level.WARNING
    java.util.logging.Logger.getLogger("org.jboss").setLevel(level)
    java.util.logging.Logger.getLogger("org.xnio").setLevel(level)
    java.util.logging.Logger.getLogger("io.undertow").setLevel(level)
  }

}

