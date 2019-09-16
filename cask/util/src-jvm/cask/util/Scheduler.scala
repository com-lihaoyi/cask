package cask.util

import java.util.concurrent.{Executors, TimeUnit}

object Scheduler{
  val scheduler = Executors.newSingleThreadScheduledExecutor()
  def schedule(millis: Long)(body: => Unit) = {
    scheduler.schedule(
      new Runnable {
        def run(): Unit = body
      },
      millis,
      TimeUnit.MILLISECONDS
    )
  }
}