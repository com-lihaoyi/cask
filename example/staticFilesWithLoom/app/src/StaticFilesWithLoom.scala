package app

import cask.internal.Util

import java.util.concurrent.{ExecutorService, Executors}

object StaticFilesWithLoom extends cask.MainRoutes{
  private val executor = Executors.newFixedThreadPool(4)

  override protected def handlerExecutor(): Option[ExecutorService] = {
    super.handlerExecutor().orElse(Some(executor))
  }
  
  @cask.get("/")
  def index() = {
    "Hello!"
  }

  @cask.staticFiles("/static/file")
  def staticFileRoutes() = "resources/cask"

  @cask.staticResources("/static/resource")
  def staticResourceRoutes() = "cask"

  @cask.staticResources("/static/resource2")
  def staticResourceRoutes2() = "."

  initialize()
}
