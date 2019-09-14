package cask.main

import cask.internal.Router
import cask.internal.Router.ArgReader
import cask.model.{Request, Response}


/**
  * Annotates a Cask endpoint that returns a HTTP [[Response]]; similar to a
  * [[Decorator]] but with additional metadata and capabilities.
  */
trait Endpoint extends BaseEndpoint {
  type OuterReturned = Router.Result[Response.Raw]
}

/**
  * An [[Endpoint]] that may return something else than a HTTP response, e.g.
  * a websocket endpoint which may instead return a websocket event handler
  */
trait BaseEndpoint extends BaseDecorator{
  /**
    * What is the path that this particular endpoint matches?
    */
  val path: String
  /**
    * Which HTTP methods does this endpoint support? POST? GET? PUT? Or some
    * combination of those?
    */
  val methods: Seq[String]

  /**
    * Whether or not this endpoint allows matching on sub-paths: does
    * `@endpoint("/foo")` capture the path "/foo/bar/baz"? Useful to e.g. have
    * an endpoint match URLs with paths in a filesystem (real or virtual) to
    * serve files
    */
  def subpath: Boolean = false

  def convertToResultType(t: InnerReturned): InnerReturned = t

  /**
    * [[Endpoint]]s are unique among decorators in that they alone can bind
    * path segments to parameters, e.g. binding `/hello/:world` to `(world: Int)`.
    * In order to do so, we need to box up the path segment strings into an
    * [[Input]] so they can later be parsed by [[getParamParser]] into an
    * instance of the appropriate type.
    */
  def wrapPathSegment(s: String): Input

}

/**
  * A [[Decorator]] that may deal with values other than HTTP [[Response]]s
  */
trait BaseDecorator{
  type Input
  type InputParser[T] <: ArgReader[Input, T, Request]
  type InnerReturned
  type Delegate = Map[String, Input] => Router.Result[InnerReturned]
  type OuterReturned <: Router.Result[Any]
  def wrapFunction(ctx: Request, delegate: Delegate): OuterReturned
  def getParamParser[T](implicit p: InputParser[T]) = p
}

/**
  * A decorator allows you to annotate a function to wrap it, via
  * `wrapFunction`. You can use this to perform additional validation before or
  * after the function runs, provide an additional parameter list of params,
  * open/commit/rollback database transactions before/after the function runs,
  * or even retrying the wrapped function if it fails.
  *
  * Calls to the wrapped function are done on the `delegate` parameter passed
  * to `wrapFunction`, which takes a `Map` representing any additional argument
  * lists (if any).
  */
trait Decorator extends BaseDecorator{
  type OuterReturned = Router.Result[Response.Raw]
  type Input = Any
  type InnerReturned = Response.Raw
  type InputParser[T] = NoOpParser[Input, T]
}

class NoOpParser[Input, T] extends ArgReader[Input, T, Request] {
  def arity = 1

  def read(ctx: Request, label: String, input: Input) = input.asInstanceOf[T]
}
object NoOpParser{
  implicit def instance[Input, T] = new NoOpParser[Input, T]
  implicit def instanceAny[T] = new NoOpParser[Any, T]
}