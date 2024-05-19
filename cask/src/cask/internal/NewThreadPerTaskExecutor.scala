package cask.internal

import java.util.concurrent.{Executor, ThreadFactory}

private[cask] final class NewThreadPerTaskExecutor(val threadFactory: ThreadFactory)
  extends Executor {
  override def execute(command: Runnable): Unit = {
    val thread = threadFactory.newThread(command)
    thread.start()
    if (thread.getState eq Thread.State.TERMINATED) {
      throw new IllegalStateException("Thread has already been terminated")
    }
  }
}
