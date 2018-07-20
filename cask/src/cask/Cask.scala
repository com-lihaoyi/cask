package cask
import cask.Router.EntryPoint

import language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox.Context
import java.io.OutputStream
import java.nio.ByteBuffer

import io.undertow.Undertow
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, HttpString}
trait RouteBase{
  val path: String
}
class get(val path: String) extends StaticAnnotation with RouteBase
class post(val path: String) extends StaticAnnotation with RouteBase
class put(val path: String) extends StaticAnnotation with RouteBase

class Main(servers: Routes*){
  val port: Int = 8080
  val host: String = "localhost"
  def main(args: Array[String]): Unit = {
    val allRoutes = for{
      server <- servers
      route <- server.caskMetadata.value.map(x => x: Routes.RouteMetadata[_])
    } yield (server, route)

    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(new HttpHandler() {
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
                bindings ++
                  exchange.getQueryParameters
                    .asScala
                    .toSeq
                    .flatMap{case (k, vs) => vs.asScala.map((k, _))}
              val result = route.entryPoint
                .asInstanceOf[EntryPoint[server.type]]
                .invoke(server, allBindings.mapValues(Some(_)).toSeq)
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
      })
      .build
    server.start()
  }
}

case class Response(data: Response.Data,
                    statusCode: Int = 200,
                    headers: Seq[(String, String)] = Nil)
object Response{
  implicit def dataResponse[T](t: T)(implicit c: T => Data) = Response(t)
  trait Data{
    def write(out: OutputStream): Unit
  }
  object Data{
    implicit class StringData(s: String) extends Data{
      def write(out: OutputStream) = out.write(s.getBytes)
    }
    implicit class BytesData(b: Array[Byte]) extends Data{
      def write(out: OutputStream) = out.write(b)
    }
  }
}
object Routes{
  case class RouteMetadata[T](metadata: RouteBase, entryPoint: EntryPoint[T])
  case class Metadata[T](value: RouteMetadata[T]*)
  object Metadata{
    implicit def initialize[T] = macro initializeImpl[T]
    implicit def initializeImpl[T: c.WeakTypeTag](c: Context): c.Expr[Metadata[T]] = {
      import c.universe._
      val router = new cask.Router(c)
      val routes = c.weakTypeOf[T].members
        .map(m => (m, m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[RouteBase])))
        .collect{case (m, Seq(a)) =>
          (
            m,
            a,
            router.extractMethod(
              m.asInstanceOf[router.c.universe.MethodSymbol],
              weakTypeOf[T].asInstanceOf[router.c.universe.Type]
            ).asInstanceOf[c.universe.Tree]
          )
        }

      val routeParts = for((m, a, routeTree) <- routes) yield {
        val annotation = q"new ${a.tree.tpe}(..${a.tree.children.tail})"
        q"cask.Routes.RouteMetadata($annotation, $routeTree)"
      }


      c.Expr[Metadata[T]](q"""cask.Routes.Metadata(..$routeParts)""")
    }
  }
}

class Routes{
  private[this] var metadata0: Routes.Metadata[this.type] = null
  def caskMetadata =
    if (metadata0 != null) metadata0
    else throw new Exception("Routes not yet initialize")

  protected[this] def initialize()(implicit routes: Routes.Metadata[this.type]): Unit = {
    metadata0 = routes
  }
}


