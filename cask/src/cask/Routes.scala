package cask
import language.experimental.macros
import java.io.OutputStream

import cask.Router.EntryPoint
import io.undertow.server.HttpServerExchange

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox.Context

class ParamType[T](val arity: Int,
                   val read0: (HttpServerExchange, Seq[String]) => T)
  extends Router.ArgReader[T, HttpServerExchange]{
  def read(ctx: HttpServerExchange, v: Seq[String]): T = read0(ctx, v)
}
object ParamType{
  def findImplicitly[T](implicit p: ParamType[T]) = p

  implicit object StringParam extends ParamType[String](1, (h, x) => x.head)
  implicit object BooleanParam extends ParamType[Boolean](1, (h, x) => x.head.toBoolean)
  implicit object ByteParam extends ParamType[Byte](1, (h, x) => x.head.toByte)
  implicit object ShortParam extends ParamType[Short](1, (h, x) => x.head.toShort)
  implicit object IntParam extends ParamType[Int](1, (h, x) => x.head.toInt)
  implicit object LongParam extends ParamType[Long](1, (h, x) => x.head.toLong)
  implicit object DoubleParam extends ParamType[Double](1, (h, x) => x.head.toDouble)
  implicit object FloatParam extends ParamType[Float](1, (h, x) => x.head.toFloat)
  implicit def SeqParam[T: ParamType] =
    new ParamType[Seq[T]](1, (h, s) => s.map(x => implicitly[ParamType[T]].read(h, Seq(x))))

  implicit object HttpExchangeParam extends ParamType[HttpServerExchange](0, (h, x) => h)
}

trait AnnotationBase{
  def wrapOutput(t: Response): Any
//  def wrapInput(t: Response): Any
}
trait RouteBase extends AnnotationBase{
  val path: String
  def wrapOutput(t: Response) = t
}
class get(val path: String) extends StaticAnnotation with RouteBase
class post(val path: String) extends StaticAnnotation with RouteBase
class put(val path: String) extends StaticAnnotation with RouteBase
class route(val path: String, val methods: Seq[String]) extends StaticAnnotation with RouteBase

class static(val path: String) extends StaticAnnotation{
  def wrapOutput(t: String) = t
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
  case class RouteMetadata[T](metadata: RouteBase,
                              entryPoint: EntryPoint[T, HttpServerExchange])
  case class Metadata[T](value: RouteMetadata[T]*)
  object Metadata{
    implicit def initialize[T] = macro initializeImpl[T]
    implicit def initializeImpl[T: c.WeakTypeTag](c: Context): c.Expr[Metadata[T]] = {
      import c.universe._
      val router = new cask.Router[c.type](c)

      val routeParts = for{
        m <- c.weakTypeOf[T].members
        annot <- m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[RouteBase])
      } yield {
        val annotObject = q"new ${annot.tree.tpe}(..${annot.tree.children.tail})"
        val route = router.extractMethod(
          m.asInstanceOf[MethodSymbol],
          weakTypeOf[T],
          (t: router.c.universe.Tree) => q"$annotObject.wrapOutput($t)",
          c.weakTypeOf[io.undertow.server.HttpServerExchange],
          q"cask.ParamType.findImplicitly"
        )

        val annotObjectSym = c.universe.TermName(c.freshName("annotObject"))
        q"""{
          val $annotObjectSym = $annotObject
          cask.Routes.RouteMetadata($annotObjectSym, $route)
        }"""
      }

      c.Expr[Metadata[T]](q"""cask.Routes.Metadata(..$routeParts)""")
    }
  }
}

trait Routes{
  private[this] var metadata0: Routes.Metadata[this.type] = null
  def caskMetadata =
    if (metadata0 != null) metadata0
    else throw new Exception("Routes not yet initialize")

  protected[this] def initialize()(implicit routes: Routes.Metadata[this.type]): Unit = {
    metadata0 = routes
  }
}

