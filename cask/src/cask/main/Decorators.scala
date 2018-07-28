package cask.main

import cask.internal.Router.ArgReader
import cask.model.ParamContext


trait Endpoint[R] extends BaseDecorator{

  val path: String
  val methods: Seq[String]
  def subpath: Boolean = false
  def wrapMethodOutput(ctx: ParamContext,t: R): cask.internal.Router.Result[Any] = {
    cask.internal.Router.Result.Success(t)
  }

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
  def getRawParams(ctx: ParamContext): Either[cask.model.Response, Decor[Input]]
  def getParamParser[T](implicit p: InputParser[T]) = p
}

object Decor{
  def apply[Input](params: (String, Input)*) = new Decor(params.toMap, () => ())
  def apply[Input](params: TraversableOnce[(String, Input)], cleanup: () => Unit = () => ()) = {
    new Decor(params.toMap, cleanup)
  }
}
class Decor[Input](val params: Map[String, Input], val cleanup: () => Unit){
  def withCleanup(newCleanUp: () => Unit) = new Decor(params, newCleanUp)
}

trait Decorator extends BaseDecorator {
  type Input = Any
  type InputParser[T] = NoOpParser[Input, T]
}

class NoOpParser[Input, T] extends ArgReader[Input, T, ParamContext] {
  def arity = 1

  def read(ctx: ParamContext, label: String, input: Input) = input.asInstanceOf[T]
}
object NoOpParser{
  implicit def instance[Input, T] = new NoOpParser[Input, T]
}