package cask.internal

import java.io.{PrintWriter, StringWriter}

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable

object Util {
  def pluralize(s: String, n: Int) = {
    if (n == 1) s else s + "s"
  }
  def splitPath(p: String) = {
    p.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse.split('/').filter(_.nonEmpty)
  }

  def stackTraceString(e: Throwable) = {
    val trace = new StringWriter()
    val pw = new PrintWriter(trace)
    e.printStackTrace(pw)
    pw.flush()
    trace.toString
  }
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
  def sequenceEither[A, B, M[X] <: TraversableOnce[X]](in: M[Either[A, B]])(
    implicit cbf: CanBuildFrom[M[Either[A, B]], B, M[B]]): Either[A, M[B]] = {
    in.foldLeft[Either[A, mutable.Builder[B, M[B]]]](Right(cbf(in))) {
      case (acc, el) =>
        for (a <- acc; e <- el) yield a += e
    }
      .map(_.result())
  }
}
