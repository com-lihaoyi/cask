package cask.main

import cask.router.RoutesEndpointsMetadata

import language.experimental.macros

trait Routes{

  def decorators = Seq.empty[cask.router.Decorator[_, _, _]]
  implicit def executionContext = concurrent.ExecutionContext.Implicits.global
  private[this] var metadata0: RoutesEndpointsMetadata[this.type] = null
  def caskMetadata =
    if (metadata0 != null) metadata0
    else throw new Exception("Routes not yet initialize")

  protected[this] def initialize()(implicit routes: RoutesEndpointsMetadata[this.type]): Unit = {
    metadata0 = routes
  }

  implicit def log: cask.util.Logger
}
