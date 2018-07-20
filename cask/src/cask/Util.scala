package cask

object Util {
  def trimSplit(p: String) =
    p.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse.split('/').filter(_.nonEmpty)

  def matchRoute(route: String, path: String): Option[Map[String, String]] = {
    val routeSegments = trimSplit(route)
    val pathSegments = trimSplit(path)

    def rec(i: Int, bindings: Map[String, String]): Option[Map[String, String]] = {
      if (routeSegments.length == i && pathSegments.length == i) Some(bindings)
      else if ((routeSegments.length == i) != (pathSegments.length == i)) None
      else {
        val routeSeg = routeSegments(i)
        val pathSeg = pathSegments(i)
        if (routeSeg(0) == ':' && routeSeg(1) == ':') {
          Some(bindings + (routeSeg.drop(2) -> pathSegments.drop(i).mkString("/")))
        }
        else if (routeSeg(0) == ':') rec(i+1, bindings + (routeSeg.drop(1) -> pathSeg))
        else if (pathSeg == routeSeg) rec(i + 1, bindings)
        else None
      }
    }
    rec(0, Map.empty)
  }
}
