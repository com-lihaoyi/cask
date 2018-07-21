package cask

object Util {
  def splitPath(p: String) =
    p.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse.split('/').filter(_.nonEmpty)
}
