package cask.endpoints

import cask.router.{HttpEndpoint, Result}
import cask.model.Request
object StaticUtil{
  def makePath(t: String, ctx: Request) = {
    (cask.internal.Util.splitPath(t) ++ ctx.remainingPathSegments)
      .filter(s => s != "." && s != "..")
      .mkString("/")
  }

}
class staticFiles(val path: String, headers: Seq[(String, String)] = Nil) extends HttpEndpoint[String, Seq[String]]{
  val methods = Seq("get")
  type InputParser[T] = QueryParamReader[T]
  override def subpath = true
  def wrapFunction(ctx: Request, delegate: Delegate) = {
    delegate(Map()).map(t => cask.model.StaticFile(StaticUtil.makePath(t, ctx), headers))
  }

  def wrapPathSegment(s: String): Seq[String] = Seq(s)
}

class staticResources(val path: String,
                      resourceRoot: ClassLoader = getClass.getClassLoader,
                      headers: Seq[(String, String)] = Nil)
  extends HttpEndpoint[String, Seq[String]]{
  val methods = Seq("get")
  type InputParser[T] = QueryParamReader[T]
  override def subpath = true
  def wrapFunction(ctx: Request, delegate: Delegate) = {
    delegate(Map()).map(t =>
      cask.model.StaticResource(StaticUtil.makePath(t, ctx), resourceRoot, headers)
    )
  }

  def wrapPathSegment(s: String): Seq[String] = Seq(s)
}
