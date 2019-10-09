package cask.router




/**
 * Represents what comes out of an attempt to invoke an [[EntryPoint]].
 * Could succeed with a value, but could fail in many different ways.
 */
sealed trait Result[+T]{
  def map[V](f: T => V): Result[V]
  def transform[V](f: PartialFunction[Any, V]): Result[V]
}
object Result{

  /**
   * Invoking the [[EntryPoint]] was totally successful, and returned a
   * result
   */
  case class Success[T](value: T) extends Result[T]{
    def map[V](f: T => V) = Success(f(value))
    def transform[V](f: PartialFunction[Any, V]) = f.lift(value) match {
      case None => Success(value).asInstanceOf[Result[V]]
      case Some(res) => Success(res)
    }
  }

  /**
   * Invoking the [[EntryPoint]] was not successful
   */
  sealed trait Error extends Result[Nothing]{
    def map[V](f: Nothing => V) = this
    def transform[V](f: PartialFunction[Any, V]) = this
  }


  object Error{


    /**
     * Invoking the [[EntryPoint]] failed with an exception while executing
     * code within it.
     */
    case class Exception(t: Throwable) extends Error

    /**
     * Invoking the [[EntryPoint]] failed because the arguments provided
     * did not line up with the arguments expected
     */
    case class MismatchedArguments(missing: Seq[ArgSig[_, _, _, _]],
                                   unknown: Seq[String]) extends Error
    /**
     * Invoking the [[EntryPoint]] failed because there were problems
     * deserializing/parsing individual arguments
     */
    case class InvalidArguments(values: Seq[ParamError]) extends Error
  }

  sealed trait ParamError
  object ParamError{
    /**
     * Something went wrong trying to de-serialize the input parameter;
     * the thrown exception is stored in [[ex]]
     */
    case class Invalid(arg: ArgSig[_, _, _, _], value: String, ex: Throwable) extends ParamError
    /**
     * Something went wrong trying to evaluate the default value
     * for this input parameter
     */
    case class DefaultFailed(arg: ArgSig[_, _, _, _], ex: Throwable) extends ParamError
  }
}