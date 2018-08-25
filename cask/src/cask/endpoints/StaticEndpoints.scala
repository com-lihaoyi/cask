package cask.endpoints

import cask.main.Endpoint
import cask.model.Request

class staticFiles(val path: String) extends Endpoint{
  type Output = String
  val methods = Seq("get")
  type Input = Seq[String]
  type InputParser[T] = QueryParamReader[T]
  override def subpath = true
  def wrapFunction(ctx: Request, delegate: Delegate): Returned = {
    delegate(Map()).map(t =>
      cask.model.StaticFile(
        (cask.internal.Util.splitPath(t) ++ ctx.remainingPathSegments)
          .filter(s => s != "." && s != "..")
          .mkString("/")
      )
    )
  }

  def wrapPathSegment(s: String): Input = Seq(s)
}

class staticResources(val path: String, resourceRoot: ClassLoader = getClass.getClassLoader) extends Endpoint{
  type Output = String
  val methods = Seq("get")
  type Input = Seq[String]
  type InputParser[T] = QueryParamReader[T]
  override def subpath = true
  def wrapFunction(ctx: Request, delegate: Delegate): Returned = {
    delegate(Map()).map(t =>
      cask.model.StaticResource(
        (cask.internal.Util.splitPath(t) ++ ctx.remainingPathSegments)
          .filter(s => s != "." && s != "..")
          .mkString("/"),
        resourceRoot
      )
    )
  }

  def wrapPathSegment(s: String): Input = Seq(s)
}
