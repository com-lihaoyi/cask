package cask.router


import scala.collection.mutable


/**
 * What is known about a single endpoint for our routes. It has a [[name]],
 * [[argSignatures]] for each argument, and a macro-generated [[invoke0]]
 * that performs all the necessary argument parsing and de-serialization.
 *
 * Realistically, you will probably spend most of your time calling [[invoke]]
 * instead, which provides a nicer API to call it that mimmicks the API of
 * calling a Scala method.
 */
case class EntryPoint[T, C](name: String,
                            argSignatures: Seq[Seq[ArgSig[_, T, _, C]]],
                            doc: Option[String],
                            invoke0: (T, C, Seq[Map[String, Any]], Seq[Seq[ArgSig[Any, _, _, C]]]) => Result[Any]){

  val firstArgs = argSignatures.head
    .map(x => x.name -> x)
    .toMap[String, ArgSig[_, T, _, C]]

  def invoke(target: T,
             ctx: C,
             paramLists: Seq[Map[String, Any]]): Result[Any] = {

    val missing = mutable.Buffer.empty[ArgSig[_, T, _, C]]

    var allowUnknownArgs = false
    for(k <- firstArgs.keys) {
      val as = firstArgs(k)
      if (!paramLists.head.contains(k)) {
        if (as.reads.arity != 0 && as.default.isEmpty) missing.append(as)
      }
      // as soon as one reader allows unknown arguments, we allow them for the
      // current parameter list
      if (as.reads.allowUnknownArgs) allowUnknownArgs = true
    }

    val unknown = if (allowUnknownArgs) Seq.empty else paramLists.head.keys.filter(!firstArgs.contains(_))

    if (missing.nonEmpty || unknown.nonEmpty) Result.Error.MismatchedArguments(missing.toSeq, unknown.toSeq)
    else {
      try invoke0(
        target,
        ctx,
        paramLists,
        argSignatures.asInstanceOf[Seq[Seq[ArgSig[Any, _, _, C]]]]
      )
      catch{case e: Throwable => Result.Error.Exception(e)}
    }
  }
}
