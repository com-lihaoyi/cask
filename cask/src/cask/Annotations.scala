package cask

import scala.annotation.StaticAnnotation


trait AnnotationBase{
  def wrapMethodOutput(t: Response): Any
  def parseMethodInput[T](implicit p: ParamReader[T]) = p
}
trait RouteBase extends AnnotationBase{
  val path: String
  def wrapMethodOutput(t: Response) = t
}
class get(val path: String) extends StaticAnnotation with RouteBase
class post(val path: String) extends StaticAnnotation with RouteBase
class put(val path: String) extends StaticAnnotation with RouteBase
class route(val path: String, val methods: Seq[String]) extends StaticAnnotation with RouteBase

class static(val path: String) extends StaticAnnotation{
  def wrapOutput(t: String) = t
}
