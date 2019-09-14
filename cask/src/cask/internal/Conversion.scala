package cask.internal

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot return ${T} as a ${V} response")
class Conversion[T, V](val f: T => V)
object Conversion{
  def create[T, V](implicit f: T => V) = new Conversion(f)
}
