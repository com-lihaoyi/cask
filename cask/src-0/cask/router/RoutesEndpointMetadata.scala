package cask.router

import cask.router.EntryPoint

case class RoutesEndpointsMetadata[T](value: EndpointMetadata[T]*)
object RoutesEndpointsMetadata{

  implicit def initialize[T]: RoutesEndpointsMetadata[T] = ???
}
