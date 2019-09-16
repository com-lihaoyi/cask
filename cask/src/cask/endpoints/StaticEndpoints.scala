package cask.endpoints

import cask.router.HttpEndpoint
import cask.model.Request

class staticFiles(val path: String) extends HttpEndpoint[String, Seq[String]]{
  val methods = Seq("get")
  type InputParser[T] = QueryParamReader[T]
  override def subpath = true
  def wrapFunction(ctx: Request, delegate: Delegate): OuterReturned = {
    delegate(Map()).map(t =>
      cask.model.StaticFile(
        (cask.internal.Util.splitPath(t) ++ ctx.remainingPathSegments)
          .filter(s => s != "." && s != "..")
          .mkString("/")
      )
    )
  }

  def wrapPathSegment(s: String): Seq[String] = Seq(s)
}

class staticResources(val path: String, resourceRoot: ClassLoader = getClass.getClassLoader)
  extends HttpEndpoint[String, Seq[String]]{
  val methods = Seq("get")
  type InputParser[T] = QueryParamReader[T]
  override def subpath = true
  def wrapFunction(ctx: Request, delegate: Delegate): OuterReturned = {
    delegate(Map()).map(t =>
      cask.model.StaticResource(
        (cask.internal.Util.splitPath(t) ++ ctx.remainingPathSegments)
          .filter(s => s != "." && s != "..")
          .mkString("/"),
        resourceRoot
      )
    )
  }

  def wrapPathSegment(s: String): Seq[String] = Seq(s)
}
