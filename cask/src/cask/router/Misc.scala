package cask.router

import scala.annotation.StaticAnnotation


class doc(s: String) extends StaticAnnotation

/**
 * Models what is known by the router about a single argument: that it has
 * a [[name]], a human-readable [[typeString]] describing what the type is
 * (just for logging and reading, not a replacement for a `TypeTag`) and
 * possible a function that can compute its default value
 */
case class ArgSig[I, -T, +V, -C](name: String,
                                 typeString: String,
                                 doc: Option[String],
                                 default: Option[T => V])
                                (implicit val reads: ArgReader[I, V, C])

trait ArgReader[I, +T, -C]{
  def arity: Int
  def unknownQueryParams: Boolean = false
  def read(ctx: C, label: String, input: I): T
}
