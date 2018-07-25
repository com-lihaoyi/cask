package cask.internal

object Util {
  def pluralize(s: String, n: Int) = {
    if (n == 1) s else s + "s"
  }
  def splitPath(p: String) =
    p.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse.split('/').filter(_.nonEmpty)

  def softWrap(s: String, leftOffset: Int, maxWidth: Int) = {
    val oneLine = s.lines.mkString(" ").split(' ')

    lazy val indent = " " * leftOffset

    val output = new StringBuilder(oneLine.head)
    var currentLineWidth = oneLine.head.length
    for(chunk <- oneLine.tail){
      val addedWidth = currentLineWidth + chunk.length + 1
      if (addedWidth > maxWidth){
        output.append("\n" + indent)
        output.append(chunk)
        currentLineWidth = chunk.length
      } else{
        currentLineWidth = addedWidth
        output.append(' ')
        output.append(chunk)
      }
    }
    output.mkString
  }
}
