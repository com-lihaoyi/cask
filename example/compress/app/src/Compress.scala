package app

import cask.internal.{ThreadBlockingHandler, Util}

import java.util.concurrent.{Executor, Executors}

object Compress extends cask.MainRoutes {

  @cask.decorators.compress
  @cask.get("/")
  def hello(): String = {
    "Hello World! Hello World! Hello World!"
  }

  initialize()
}
