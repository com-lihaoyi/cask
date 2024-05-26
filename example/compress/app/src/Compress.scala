package app

import cask.internal.{ThreadBlockingHandler, Util}

import java.util.concurrent.Executor

object Compress extends cask.MainRoutes {

  protected override val handlerExecutor: Executor = {
    if (System.getProperty("cask.virtualThread.enabled", "false").toBoolean) {
      Util.createNewThreadPerTaskExecutor(
        Util.createVirtualThreadFactory("cask-handler-executor"))
    } else null
  }

  @cask.decorators.compress
  @cask.get("/")
  def hello(): String = {
    "Hello World! Hello World! Hello World!"
  }

  initialize()
}
