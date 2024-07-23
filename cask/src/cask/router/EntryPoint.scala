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
                            invoke0: (T, Seq[C], Seq[Map[String, Any]], Seq[Seq[ArgSig[Any, _, _, C]]]) => Result[Any]){

  val firstArgs = argSignatures.head
    .map(x => x.name -> x)
    .toMap[String, ArgSig[_, T, _, C]]

  def invoke(target: T,
             ctxs: Seq[C],
             paramLists: Seq[Map[String, Any]]): Result[Any] = {

    val missing = mutable.Buffer.empty[ArgSig[_, T, _, C]]

    val unknown = paramLists.head.keys.filter(!firstArgs.contains(_))

    for(k <- firstArgs.keys) {
      if (!paramLists.head.contains(k)) {
        val as = firstArgs(k)
        if (as.reads.arity > 0 && as.default.isEmpty) missing.append(as)
      }
    }

    if (missing.nonEmpty || (!argSignatures.exists(_.exists(_.reads.unknownQueryParams)) && unknown.nonEmpty)) {
      Result.Error.MismatchedArguments(missing.toSeq, unknown.toSeq)
    } else {
      try invoke0(
        target,
        ctxs,
        paramLists,
        argSignatures.asInstanceOf[Seq[Seq[ArgSig[Any, _, _, C]]]]
      )
      catch{case e: Throwable => Result.Error.Exception(e)}
    }
  }
}
