package cask.internal

import language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.collection.mutable
import scala.reflect.macros.blackbox.Context

/**
  * More or less a minimal version of Autowire's Server that lets you generate
  * a set of "routes" from the methods defined in an object, and call them
  * using passing in name/args/kwargs via Java reflection, without having to
  * generate/compile code or use Scala reflection. This saves us spinning up
  * the Scala compiler and greatly reduces the startup time of cached scripts.
  */
object Router{
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
    def read(ctx: C, label: String, input: I): T
  }

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
      .toMap[String, Router.ArgSig[_, T, _, C]]

    def invoke(target: T,
               ctx: C,
               paramLists: Seq[Map[String, Any]]): Result[Any] = {

      val missing = mutable.Buffer.empty[Router.ArgSig[_, T, _, C]]

      val unknown = paramLists.head.keys.filter(!firstArgs.contains(_))

      for(k <- firstArgs.keys) {
        if (!paramLists.head.contains(k)) {
          val as = firstArgs(k)
          if (as.reads.arity != 0 && as.default.isEmpty) missing.append(as)
        }
      }

      if (missing.nonEmpty || unknown.nonEmpty) Result.Error.MismatchedArguments(missing, unknown.toSeq)
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

  def tryEither[T](t: => T, error: Throwable => Result.ParamError) = {
    try Right(t)
    catch{ case e: Throwable => Left(error(e))}
  }

  /**
    * Represents what comes out of an attempt to invoke an [[EntryPoint]].
    * Could succeed with a value, but could fail in many different ways.
    */
  sealed trait Result[+T]{
    def map[V](f: T => V): Result[V]
  }
  object Result{

    /**
      * Invoking the [[EntryPoint]] was totally successful, and returned a
      * result
      */
    case class Success[T](value: T) extends Result[T]{
      def map[V](f: T => V) = Success(f(value))
    }

    /**
      * Invoking the [[EntryPoint]] was not successful
      */
    sealed trait Error extends Result[Nothing]{
      def map[V](f: Nothing => V) = this
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


class Router[C <: Context](val c: C) {
  import c.universe._
  def getValsOrMeths(curCls: Type): Iterable[MethodSymbol] = {
    def isAMemberOfAnyRef(member: Symbol) = {
      // AnyRef is an alias symbol, we go to the real "owner" of these methods
      val anyRefSym = c.mirror.universe.definitions.ObjectClass
      member.owner == anyRefSym
    }
    val extractableMembers = for {
      member <- curCls.members.toList.reverse
      if !isAMemberOfAnyRef(member)
      if !member.isSynthetic
      if member.isPublic
      if member.isTerm
      memTerm = member.asTerm
      if memTerm.isMethod
      if !memTerm.isModule
    } yield memTerm.asMethod

    extractableMembers flatMap { case memTerm =>
      if (memTerm.isSetter || memTerm.isConstructor || memTerm.isGetter) Nil
      else Seq(memTerm)

    }
  }



  def unwrapVarargType(arg: Symbol) = {
    val vararg = arg.typeSignature.typeSymbol == definitions.RepeatedParamClass
    val unwrappedType =
      if (!vararg) arg.typeSignature
      else arg.typeSignature.asInstanceOf[TypeRef].args(0)

    (vararg, unwrappedType)
  }

  def extractMethod(method: MethodSymbol,
                    curCls: c.universe.Type,
                    convertToResultType: c.Tree,
                    ctx: c.Tree,
                    argReaders: Seq[c.Tree],
                    annotDeserializeTypes: Seq[c.Tree]): c.universe.Tree = {
    val baseArgSym = TermName(c.freshName())

    def getDocAnnotation(annotations: List[Annotation]) = {
      val (docTrees, remaining) = annotations.partition(_.tpe =:= typeOf[Router.doc])
      val docValues = for {
        doc <- docTrees
        if doc.scalaArgs.head.isInstanceOf[Literal]
        l = doc.scalaArgs.head.asInstanceOf[Literal]
        if l.value.value.isInstanceOf[String]
      } yield l.value.value.asInstanceOf[String]
      (remaining, docValues.headOption)
    }
    val (_, methodDoc) = getDocAnnotation(method.annotations)
    val argValuesSymbol = q"${c.fresh[TermName]("argValues")}"
    val argSigsSymbol = q"${c.fresh[TermName]("argSigs")}"
    val ctxSymbol = q"${c.fresh[TermName]("ctx")}"
    val argData = for(argListIndex <- method.paramLists.indices) yield{
      val annotDeserializeType = annotDeserializeTypes.lift(argListIndex).getOrElse(tq"scala.Any")
      val argReader = argReaders.lift(argListIndex).getOrElse(q"cask.main.NoOpParser.instanceAny")
      val flattenedArgLists = method.paramss(argListIndex)
      def hasDefault(i: Int) = {
        val defaultName = s"${method.name}$$default$$${i + 1}"
        if (curCls.members.exists(_.name.toString == defaultName)) Some(defaultName)
        else None
      }

      val defaults = for (i <- flattenedArgLists.indices) yield {
        val arg = TermName(c.freshName())
        hasDefault(i).map(defaultName => q"($arg: $curCls) => $arg.${newTermName(defaultName)}")
      }

      val readArgSigs = for (
        ((arg, defaultOpt), i) <- flattenedArgLists.zip(defaults).zipWithIndex
      ) yield {

        if (arg.typeSignature.typeSymbol == definitions.RepeatedParamClass) c.abort(method.pos, "Varargs are not supported in cask routes")

        val default = defaultOpt match {
          case Some(defaultExpr) => q"scala.Some($defaultExpr($baseArgSym))"
          case None => q"scala.None"
        }

        val (docUnwrappedType, docOpt) = arg.typeSignature match {
          case t: AnnotatedType =>
            import compat._
            val (remaining, docValue) = getDocAnnotation(t.annotations)
            if (remaining.isEmpty) (t.underlying, docValue)
            else (c.universe.AnnotatedType(remaining, t.underlying), docValue)

          case t => (t, None)
        }

        val docTree = docOpt match {
          case None => q"scala.None"
          case Some(s) => q"scala.Some($s)"
        }

        val argSig =
          q"""
          cask.internal.Router.ArgSig[$annotDeserializeType, $curCls, $docUnwrappedType, $ctx](
            ${arg.name.toString},
            ${docUnwrappedType.toString},
            $docTree,
            $defaultOpt
          )($argReader[$docUnwrappedType])
        """

        val reader = q"""
          cask.internal.Router.makeReadCall(
            $argValuesSymbol($argListIndex),
            $ctxSymbol,
            $default,
            $argSigsSymbol($argListIndex)($i)
          )
        """

        c.internal.setPos(reader, method.pos)
        (reader, argSig)
      }

      val (readArgs, argSigs) = readArgSigs.unzip
      val (argNames, argNameCasts) = flattenedArgLists.map { arg =>
        val (vararg, unwrappedType) = unwrapVarargType(arg)
        (
          pq"${arg.name.toTermName}",
          if (!vararg) q"${arg.name.toTermName}.asInstanceOf[$unwrappedType]"
          else q"${arg.name.toTermName}.asInstanceOf[Seq[$unwrappedType]]: _*"

        )
      }.unzip

      (argNameCasts, argSigs, argNames, readArgs)
    }

    val argNameCasts = argData.map(_._1)
    val argSigs = argData.map(_._2)
    val argNames = argData.map(_._3)
    val readArgs = argData.map(_._4)
    var methodCall: c.Tree = q"$baseArgSym.${method.name.toTermName}"
    for(argNameCast <- argNameCasts) methodCall = q"$methodCall(..$argNameCast)"

    val res = q"""
    cask.internal.Router.EntryPoint[$curCls, $ctx](
      ${method.name.toString},
      ${argSigs.toList},
      ${methodDoc match{
        case None => q"scala.None"
        case Some(s) => q"scala.Some($s)"
      }},
      (
        $baseArgSym: $curCls,
        $ctxSymbol: $ctx,
        $argValuesSymbol: Seq[Map[String, Any]],
        $argSigsSymbol: scala.Seq[scala.Seq[cask.internal.Router.ArgSig[Any, _, _, $ctx]]]
      ) =>
        cask.internal.Router.validate(Seq(..${readArgs.flatten.toList})).map{
          case Seq(..${argNames.flatten.toList}) => $convertToResultType($methodCall)
        }
    )
    """

    c.internal.transform(res){(t, a) =>
      c.internal.setPos(t, method.pos)
      a.default(t)
    }

    res
  }

}
