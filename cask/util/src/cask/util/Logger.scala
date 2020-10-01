package cask.util

import sourcecode.{File, Line, Text}

trait Logger {
  def exception(t: Throwable): Unit

  def debug(t: sourcecode.Text[Any])(implicit f: sourcecode.File, line: sourcecode.Line): Unit
}
object Logger{
  object Console {
    implicit object globalLogger extends Console()
  }
  class Console() extends Logger{
    def exception(t: Throwable): Unit = t.printStackTrace()

    def debug(t: Text[Any])(implicit f: File, line: Line): Unit = {
      println(f.value.split('/').last + ":" + line + " " + t.source + " " + pprint.apply(t.value))
    }
  }
}
