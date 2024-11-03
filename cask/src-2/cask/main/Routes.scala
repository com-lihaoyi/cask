package cask.main

import cask.router.RoutesEndpointsMetadata

import language.experimental.macros

trait Routes{

  def decorators = Seq.empty[cask.router.Decorator[_, _, _, _]]
  private[this] var metadata0: RoutesEndpointsMetadata[this.type] = null
  def caskMetadata =
    if (metadata0 != null) metadata0
    else throw new Exception("Routes not yet initialized")

  protected[this] def initialize()(implicit routes: RoutesEndpointsMetadata[this.type]): Unit = {
    metadata0 = routes
  }

}
