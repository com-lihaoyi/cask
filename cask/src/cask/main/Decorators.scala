package cask.main

import cask.internal.Router
import cask.internal.Router.ArgReader
import cask.model.{Response, ParamContext}


trait Endpoint[R] extends BaseDecorator{

  type Output = R
  val path: String
  val methods: Seq[String]
  def subpath: Boolean = false

  def wrapMethodOutput0(ctx: ParamContext, t: R): cask.internal.Router.Result[Any] = {
    cask.internal.Router.Result.Success(t)
  }
  def wrapMethodOutput(ctx: ParamContext,
                       delegate: Map[String, Input] => Router.Result[Output]): Router.Result[Response]

  def wrapPathSegment(s: String): Input

}

/**
  * The core interface of decorator annotations: the decorator provides "raw"
  * values to the annotated function via `getRawParams`, which then get
  * processed by `getParamParser` into the correct argument types before
  * being passed to the function.
  *
  * For a trivial "provide value" decorator, `getRawParams` would return the
  * final param value and `getParamParser` would return a no-op parser. For
  * a decorator that takes its input as query-params, JSON, or similar,
  * `getRawParams` would provide raw query/JSON/etc. values and
  * `getParamParser` would be responsible for processing those into the
  * correct parameter types.
  */
trait BaseDecorator{
  type Input
  type InputParser[T] <: ArgReader[Input, T, ParamContext]
  type Output
  def wrapMethodOutput(ctx: ParamContext,
                       delegate: Map[String, Input] => Router.Result[Output]): Router.Result[Response]
  def getParamParser[T](implicit p: InputParser[T]) = p
}


trait Decorator extends BaseDecorator {
  type Input = Any
  type Output = Response
  type InputParser[T] = NoOpParser[Input, T]
}

class NoOpParser[Input, T] extends ArgReader[Input, T, ParamContext] {
  def arity = 1

  def read(ctx: ParamContext, label: String, input: Input) = input.asInstanceOf[T]
}
object NoOpParser{
  implicit def instance[Input, T] = new NoOpParser[Input, T]
}