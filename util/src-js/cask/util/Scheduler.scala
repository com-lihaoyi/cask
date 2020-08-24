package cask.util
object Scheduler{
  def schedule(millis: Long)(body: => Unit) = {
    scala.scalajs.js.timers.setTimeout(millis)(body)
  }
}
