package cask.main

import java.io.{PrintWriter, StringWriter}

import cask.model._
import cask.internal.Router.EntryPoint
import cask.internal.{DispatchTrie, Router, Util}
import io.undertow.Undertow
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.HttpString
import fastparse.utils.Utils.literalize

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

  def handleError(statusCode: Int): Response = {
    Response(
      s"Error $statusCode: ${Status.codesToStatus(statusCode).reason}",
      statusCode = statusCode
    )
  }

  def writeResponse(exchange: HttpServerExchange, response: BaseResponse) = {
    response.headers.foreach{case (k, v) =>
      exchange.getResponseHeaders.put(new HttpString(k), v)
    }
    response.cookies.foreach(c => exchange.setResponseCookie(Cookie.toUndertow(c)))

    exchange.setStatusCode(response.statusCode)
    response.data.write(exchange.getOutputStream)
  }

  def getLeftColWidth(items: Seq[Router.ArgSig[_, _, _,_]]) = {
    items.map(_.name.length + 2) match{
      case Nil => 0
      case x => x.max
    }
  }

  def renderArg[T](base: T,
                   arg: Router.ArgSig[_, T, _, _],
                   leftOffset: Int,
                   wrappedWidth: Int): (String, String) = {
    val suffix = arg.default match{
      case Some(f) => " (default " + f(base) + ")"
      case None => ""
    }
    val docSuffix = arg.doc match{
      case Some(d) => ": " + d
      case None => ""
    }
    val wrapped = Util.softWrap(
      arg.typeString + suffix + docSuffix,
      leftOffset,
      wrappedWidth - leftOffset
    )
    (arg.name, wrapped)
  }

  def formatMainMethodSignature[T](base: T,
                                   main: Router.EntryPoint[_, T, _],
                                   leftIndent: Int,
                                   leftColWidth: Int) = {
    // +2 for space on right of left col
    val args = main.argSignatures.map(renderArg(base, _, leftColWidth + leftIndent + 2 + 2, 80))

    val leftIndentStr = " " * leftIndent
    val argStrings =
      for((lhs, rhs) <- args)
        yield {
          val lhsPadded = lhs.padTo(leftColWidth, ' ')
          val rhsPadded = rhs.lines.mkString("\n")
          s"$leftIndentStr  $lhsPadded  $rhsPadded"
        }
    val mainDocSuffix = main.doc match{
      case Some(d) => "\n" + leftIndentStr + Util.softWrap(d, leftIndent, 80)
      case None => ""
    }

    s"""$leftIndentStr${main.name}$mainDocSuffix
       |${argStrings.map(_ + "\n").mkString}""".stripMargin
  }

  def formatInvokeError[T](base: T, route: Router.EntryPoint[_, T, _], x: Router.Result.Error): String = {
    def expectedMsg = formatMainMethodSignature(base: T, route, 0, 0)

    x match{
      case Router.Result.Error.Exception(x) => ???
      case Router.Result.Error.MismatchedArguments(missing, unknown) =>
        val missingStr =
          if (missing.isEmpty) ""
          else {
            val chunks =
              for (x <- missing)
                yield x.name + ": " + x.typeString

            val argumentsStr = Util.pluralize("argument", chunks.length)
            s"Missing $argumentsStr: (${chunks.mkString(", ")})\n"
          }


        val unknownStr =
          if (unknown.isEmpty) ""
          else {
            val argumentsStr = Util.pluralize("argument", unknown.length)
            s"Unknown $argumentsStr: " + unknown.map(literalize(_)).mkString(" ") + "\n"
          }


        s"""$missingStr$unknownStr
           |Arguments provided did not match expected signature:
           |
           |$expectedMsg
           |""".stripMargin

      case Router.Result.Error.InvalidArguments(x) =>
        val argumentsStr = Util.pluralize("argument", x.length)
        val thingies = x.map{
          case Router.Result.ParamError.Invalid(p, v, ex) =>
            val literalV = literalize(v)
            val trace = new StringWriter()
            ex.printStackTrace(new PrintWriter(trace))
            s"${p.name}: ${p.typeString} = $literalV failed to parse with $ex\n$trace"
          case Router.Result.ParamError.DefaultFailed(p, ex) =>
            val trace = new StringWriter()
            ex.printStackTrace(new PrintWriter(trace))
            s"${p.name}'s default value failed to evaluate with $ex\n$trace"
        }

        s"""The following $argumentsStr failed to parse:
           |
           |${thingies.mkString("\n")}
           |
           |expected signature:
           |
           |$expectedMsg
           |""".stripMargin

    }
  }
  lazy val defaultHandler = new HttpHandler() {
    def handleRequest(exchange: HttpServerExchange): Unit = {
      routeTries(exchange.getRequestMethod.toString.toLowerCase()).lookup(Util.splitPath(exchange.getRequestPath).toList, Map()) match{
        case None => writeResponse(exchange, handleError(404))
        case Some(((routes, metadata), bindings, remaining)) =>
          val result = metadata.endpoint.handle(
            ParamContext(exchange, remaining), bindings, routes,
            metadata.entryPoint.asInstanceOf[
              EntryPoint[metadata.endpoint.InputType, cask.main.Routes, cask.model.ParamContext]
            ]
          )

          result match{
            case Router.Result.Success(response) => writeResponse(exchange, response)
            case e: Router.Result.Error =>

              writeResponse(exchange,
                Response(
                  formatInvokeError(
                    routes,
                    metadata.entryPoint.asInstanceOf[EntryPoint[_, cask.main.Routes, _]],
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


