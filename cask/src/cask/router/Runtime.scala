package cask.router

object Runtime{

  def tryEither[T](t: => T, error: Throwable => Result.ParamError) = {
    try Right(t)
    catch{ case e: Throwable => Left(error(e))}
  }


  def validate(args: Seq[Either[Seq[Result.ParamError], Any]]): Result[Seq[Any]] = {
    val lefts = args.collect{case Left(x) => x}.flatten

    if (lefts.nonEmpty) Result.Error.InvalidArguments(lefts)
    else {
      val rights = args.collect{case Right(x) => x}
      Result.Success(rights)
    }
  }

  def makeReadCall[I, C](dict: Map[String, I],
                         ctx: C,
                         default: => Option[Any],
                         arg: ArgSig[I, _, _, C]) = {
    arg.reads.arity match{
      case 0 =>
        tryEither(
          arg.reads.read(ctx, arg.name, null.asInstanceOf[I]), Result.ParamError.DefaultFailed(arg, _)).left.map(Seq(_))
      case 1 =>
        dict.get(arg.name) match{
          case None =>
            tryEither(default.get, Result.ParamError.DefaultFailed(arg, _)).left.map(Seq(_))

          case Some(x) =>
            tryEither(arg.reads.read(ctx, arg.name, x), Result.ParamError.Invalid(arg, x.toString, _)).left.map(Seq(_))
        }
    }
  }
}