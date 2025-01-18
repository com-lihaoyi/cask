package cask.main

import cask.endpoints.{WebsocketResult, WsHandler}
import cask.internal.{DispatchTrie, ThreadBlockingHandler, Util}
import cask.model._
import cask.router.{Decorator, EndpointMetadata, EntryPoint, Result}
import cask.util.Logger
import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.HttpString

import java.util.concurrent.ExecutorService

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
  def mainDecorators: Seq[Decorator[_, _, _, _]] = Nil
  def allRoutes: Seq[Routes]
  def port: Int = 8080
  def host: String = "localhost"
  def verbose = false
  def debugMode: Boolean = true

  def createExecutionContext = castor.Context.Simple.executionContext
  def createActorContext = new castor.Context.Simple(executionContext, log.exception)

  val executionContext = createExecutionContext
  implicit val actorContext: castor.Context = createActorContext

  implicit def log: cask.util.Logger = new cask.util.Logger.Console()

  private lazy val cachedHandlerExecutor: Option[ExecutorService] = handlerExecutor()

  def dispatchTrie = Main.prepareDispatchTrie(allRoutes)

  /**
   * The handler that will be used to handle incoming requests. By default,
   * when a handler is provided, a default handler will be used,
   * otherwise the provided executor will be used to handle requests.
   *
   * When `cask.virtual-threads.enabled` is set to `true` and running with a JDK
   * where virtual threads are supported, then a virtual thread executor will be used.
   * */
  protected def handlerExecutor(): Option[ExecutorService] = {
    for {
      config <- Option(System.getProperty(Main.VIRTUAL_THREAD_ENABLED))
      if config.toBoolean
      executor <- Util.createDefaultCaskVirtualThreadExecutor
    } yield executor
  }

  def defaultHandler: HttpHandler = {
    val mainHandler = new Main.DefaultHandler(
      dispatchTrie, mainDecorators, debugMode, handleNotFound, handleMethodNotAllowed, handleEndpointError)
    cachedHandlerExecutor match {
      case None => new BlockingHandler(mainHandler)
      case Some(executor) => new ThreadBlockingHandler(executor, mainHandler)
    }
  }

  def handleNotFound(req: Request): Response.Raw = Main.defaultHandleNotFound(req)

  def handleMethodNotAllowed(req: Request): Response.Raw = Main.defaultHandleMethodNotAllowed(req)

  def handleEndpointError(routes: Routes,
                          metadata: EndpointMetadata[_],
                          e: cask.router.Result.Error,
                          req: Request): Response.Raw = {
    Main.defaultHandleError(routes, metadata, e, debugMode, req)
  }

  def main(args: Array[String]): Unit = {
    if (!verbose) Main.silenceJboss()
    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(defaultHandler)
      .build
    server.start()
    //register an on exit hook to stop the server
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      server.stop()
      cachedHandlerExecutor.foreach(_.shutdown())
      executionContext.shutdown()
    }))
  }

}

object Main {
  /**
   * property key to enable virtual thread support.
   * */
  val VIRTUAL_THREAD_ENABLED = "cask.virtual-threads.enabled"

  class DefaultHandler(dispatchTrie: DispatchTrie[Map[String, (Routes, EndpointMetadata[_])]],
                       mainDecorators: Seq[Decorator[_, _, _, _]],
                       debugMode: Boolean,
                       handleNotFound: Request => Response.Raw,
                       handleMethodNotAllowed: Request => Response.Raw,
                       handleError: (Routes, EndpointMetadata[_], Result.Error, Request) => Response.Raw)
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

      dispatchTrie.lookup(decodedSegments, Vector()) match {
        case None => Main.writeResponse(exchange, handleNotFound(Request(exchange, decodedSegments, Map())))
        case Some((methodMap, routeBindings, remaining)) =>
          methodMap.get(effectiveMethod) match {
            case None => Main.writeResponse(exchange, handleMethodNotAllowed(Request(exchange, remaining, routeBindings)))
            case Some((routes, metadata)) =>
              val req = Request(exchange, remaining, routeBindings)
              Decorator.invoke(
                req,
                metadata.endpoint,
                metadata.entryPoint.asInstanceOf[EntryPoint[Routes, _]],
                routes,
                (mainDecorators ++ routes.decorators ++ metadata.decorators).toList,
                Nil,
                Nil
              ) match {
                case Result.Success(res) => runner(res)
                case e: Result.Error =>
                  Main.writeResponse(
                    exchange,
                    handleError(routes, metadata, e, req)
                  )
              }
          }
      }
    } catch {
      case e: Throwable =>
        e.printStackTrace()
    }
  }

  def defaultHandleNotFound(req: Request): Response.Raw = {
    Response(
      s"Error 404: ${Status.codesToStatus(404).reason}",
      statusCode = 404
    )
  }

  def defaultHandleMethodNotAllowed(req: Request): Response.Raw = {
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
      val methodMap = values.map(_._2).flatten
      val hasSubpath = values.map(_._3).contains(true)
      (segments, methodMap, hasSubpath)
    }.toSeq

    DispatchTrie.construct(0, dispatchInputs)(_.map(_._1)).map(_.toMap)
  }

  def writeResponse(exchange: HttpServerExchange, response: Response.Raw) = {
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
      override def close() = {
        if (!exchange.isComplete) output.close()
      }
      override def flush() = {
        if (!exchange.isComplete) output.flush()
      }
    })
  }

  def defaultHandleError(routes: Routes,
                         metadata: EndpointMetadata[_],
                         e: Result.Error,
                         debugMode: Boolean,
                         req: Request)
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

