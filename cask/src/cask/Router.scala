package cask


import io.undertow.server.HttpServerExchange

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
  case class ArgSig[-T, +V, -C](name: String,
                                typeString: String,
                                doc: Option[String],
                                default: Option[T => V])
                               (implicit val reads: ArgReader[V, C])

  type AnyArgReader = ArgReader[Any, Nothing]

  trait ArgReader[+T, -C]{
    def arity: Int
    def read(ctx: C, input: Seq[String]): T
  }

  def stripDashes(s: String) = {
    if (s.startsWith("--")) s.drop(2)
    else if (s.startsWith("-")) s.drop(1)
    else s
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
                              argSignatures: Seq[ArgSig[T, _, C]],
                              doc: Option[String],
                              varargs: Boolean,
                              invoke0: (T, C, Map[String, Seq[String]], Seq[String]) => Result[Any]){
      def invoke(target: T, ctx: C, groupedArgs: Seq[(String, Option[String])]): Result[Any] = {
      var remainingArgSignatures = argSignatures.toList.filter(_.reads.arity > 0)

      val accumulatedKeywords = mutable.Map.empty[ArgSig[T, _, C], mutable.Buffer[String]]
      val keywordableArgs = if (varargs) argSignatures.dropRight(1) else argSignatures

      for(arg <- keywordableArgs) accumulatedKeywords(arg) = mutable.Buffer.empty

      val leftoverArgs = mutable.Buffer.empty[String]

      val lookupArgSig = Map(argSignatures.map(x => (x.name, x)):_*)

      var incomplete: Option[ArgSig[T, _, C]] = None

      for(group <- groupedArgs){

        group match{
          case (value, None) =>
            if (value(0) == '-' && !varargs){
              lookupArgSig.get(stripDashes(value)) match{
                case None => leftoverArgs.append(value)
                case Some(sig) => incomplete = Some(sig)
              }

            } else remainingArgSignatures match {
              case Nil => leftoverArgs.append(value)
              case last :: Nil if varargs => leftoverArgs.append(value)
              case next :: rest =>
                accumulatedKeywords(next).append(value)
                remainingArgSignatures = rest
            }
          case (rawKey, Some(value)) =>
            val key = stripDashes(rawKey)
            lookupArgSig.get(key) match{
              case Some(x) if accumulatedKeywords.contains(x) =>
                if (accumulatedKeywords(x).nonEmpty && varargs){
                  leftoverArgs.append(rawKey, value)
                }else{
                  accumulatedKeywords(x).append(value)
                  remainingArgSignatures = remainingArgSignatures.filter(_.name != key)
                }
              case _ =>
                leftoverArgs.append(rawKey, value)
            }
        }
      }

      val missing0 = remainingArgSignatures
        .filter(_.default.isEmpty)

      val missing = if(varargs) {
        missing0.filter(_ != argSignatures.last)
      } else {
        missing0.filter(x => incomplete != Some(x))
      }

      if (
        incomplete.nonEmpty ||
          missing.nonEmpty ||
          (leftoverArgs.nonEmpty && !varargs)
      ){
        Result.Error.MismatchedArguments(
          missing = missing,
          unknown = leftoverArgs,
          duplicate = Nil,
          incomplete = incomplete

        )
      } else {
        val mapping = accumulatedKeywords
          .map{case (k, single) => (k.name, single)}
          .toMap

        try invoke0(target, ctx, mapping, leftoverArgs)
        catch{case e: Throwable => Result.Error.Exception(e)}
      }
    }
  }

  def tryEither[T](t: => T, error: Throwable => Result.ParamError) = {
    try Right(t)
    catch{ case e: Throwable => Left(error(e))}
  }

  def read[C]
          (dict: Map[String, Seq[String]],
           default: => Option[Any],
           arg: ArgSig[_, _, C],
           thunk: Seq[String] => Any): FailMaybe = {
    arg.reads.arity match{
      case 0 =>
        tryEither(thunk(null), Result.ParamError.DefaultFailed(arg, _)).left.map(Seq(_))
      case 1 =>
        dict.get(arg.name) match{
          case None =>
            tryEither(default.get, Result.ParamError.DefaultFailed(arg, _)).left.map(Seq(_))

          case Some(x) =>
            tryEither(thunk(x), Result.ParamError.Invalid(arg, x, _)).left.map(Seq(_))
        }
    }

  }

  /**
    * Represents what comes out of an attempt to invoke an [[EntryPoint]].
    * Could succeed with a value, but could fail in many different ways.
    */
  sealed trait Result[+T]
  object Result{

    /**
      * Invoking the [[EntryPoint]] was totally successful, and returned a
      * result
      */
    case class Success[T](value: T) extends Result[T]

    /**
      * Invoking the [[EntryPoint]] was not successful
      */
    sealed trait Error extends Result[Nothing]


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
      case class MismatchedArguments(missing: Seq[ArgSig[_, _, _]],
                                     unknown: Seq[String],
                                     duplicate: Seq[(ArgSig[_, _, _], Seq[String])],
                                     incomplete: Option[ArgSig[_, _, _]]) extends Error
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
      case class Invalid(arg: ArgSig[_, _, _], value: Seq[String], ex: Throwable) extends ParamError
      /**
        * Something went wrong trying to evaluate the default value
        * for this input parameter
        */
      case class DefaultFailed(arg: ArgSig[_, _, _], ex: Throwable) extends ParamError
    }
  }


  type FailMaybe = Either[Seq[Result.ParamError], Any]
  type FailAll = Either[Seq[Result.ParamError], Seq[Any]]

  def validate(args: Seq[FailMaybe]): Result[Seq[Any]] = {
    val lefts = args.collect{case Left(x) => x}.flatten

    if (lefts.nonEmpty) Result.Error.InvalidArguments(lefts)
    else {
      val rights = args.collect{case Right(x) => x}
      Result.Success(rights)
    }
  }

  def makeReadCall[C]
                  (dict: Map[String, Seq[String]],
                   ctx: C,
                   default: => Option[Any],
                   arg: ArgSig[_, _, C]) = {
    read(dict, default, arg, arg.reads.read(ctx, _))
  }

}


class Router [C <: Context](val c: C) {
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

  def extractMethod(meth: MethodSymbol,
                    curCls: c.universe.Type,
                    wrapOutput: c.Tree => c.Tree,
                    ctx: c.Type,
                    argReader: c.Tree): c.universe.Tree = {
    val baseArgSym = TermName(c.freshName())
    val flattenedArgLists = meth.paramss.flatten
    def hasDefault(i: Int) = {
      val defaultName = s"${meth.name}$$default$$${i + 1}"
      if (curCls.members.exists(_.name.toString == defaultName)) Some(defaultName)
      else None
    }
    val argListSymbol = q"${c.fresh[TermName]("argsList")}"
    val extrasSymbol = q"${c.fresh[TermName]("extras")}"
    val defaults = for ((arg, i) <- flattenedArgLists.zipWithIndex) yield {
      val arg = TermName(c.freshName())
      hasDefault(i).map(defaultName => q"($arg: $curCls) => $arg.${newTermName(defaultName)}")
    }

    def getDocAnnotation(annotations: List[Annotation]) = {
      val (docTrees, remaining) = annotations.partition(_.tpe =:= typeOf[Router.doc])
      val docValues = for {
        doc <- docTrees
        if doc.scalaArgs.head.isInstanceOf[Literal]
        l =  doc.scalaArgs.head.asInstanceOf[Literal]
        if l.value.value.isInstanceOf[String]
      } yield l.value.value.asInstanceOf[String]
      (remaining, docValues.headOption)
    }

    def unwrapVarargType(arg: Symbol) = {
      val vararg = arg.typeSignature.typeSymbol == definitions.RepeatedParamClass
      val unwrappedType =
        if (!vararg) arg.typeSignature
        else arg.typeSignature.asInstanceOf[TypeRef].args(0)

      (vararg, unwrappedType)
    }


    val (_, methodDoc) = getDocAnnotation(meth.annotations)
    val readArgSigs = for(
      ((arg, defaultOpt), i) <- flattenedArgLists.zip(defaults).zipWithIndex
    ) yield {

      val (vararg, varargUnwrappedType) = unwrapVarargType(arg)

      val default =
        if (vararg) q"scala.Some(scala.Nil)"
        else defaultOpt match {
          case Some(defaultExpr) => q"scala.Some($defaultExpr($baseArgSym))"
          case None => q"scala.None"
        }

      val (docUnwrappedType, docOpt) = varargUnwrappedType match{
        case t: AnnotatedType =>
          import compat._
          val (remaining, docValue) = getDocAnnotation(t.annotations)
          if (remaining.isEmpty) (t.underlying, docValue)
          else (c.universe.AnnotatedType(remaining, t.underlying), docValue)

        case t => (t, None)
      }

      val docTree = docOpt match{
        case None => q"scala.None"
        case Some(s) => q"scala.Some($s)"
      }

      val argSig = q"""
        cask.Router.ArgSig[$curCls, $docUnwrappedType, $ctx](
          ${arg.name.toString},
          ${docUnwrappedType.toString + (if(vararg) "*" else "")},
          $docTree,
          $defaultOpt
        )($argReader[$docUnwrappedType])
      """

      val reader =
        if(vararg) c.abort(meth.pos, "Varargs are not supported in cask routes")
        else q"""
          cask.Router.makeReadCall(
            $argListSymbol,
            ctx,
            $default,
            $argSig
          )
        """
      c.internal.setPos(reader, meth.pos)
      (reader, argSig, vararg)
    }

    val (readArgs, argSigs, varargs) = readArgSigs.unzip3
    val (argNames, argNameCasts) = flattenedArgLists.map { arg =>
      val (vararg, unwrappedType) = unwrapVarargType(arg)
      (
        pq"${arg.name.toTermName}",
        if (!vararg) q"${arg.name.toTermName}.asInstanceOf[$unwrappedType]"
        else q"${arg.name.toTermName}.asInstanceOf[Seq[$unwrappedType]]: _*"

      )
    }.unzip


    val methCall =
      if (meth.paramLists.isEmpty) q"$baseArgSym.${meth.name.toTermName}"
      else q"$baseArgSym.${meth.name.toTermName}(..$argNameCasts)"
    val res = q"""
    cask.Router.EntryPoint[$curCls, $ctx](
      ${meth.name.toString},
      scala.Seq(..$argSigs),
      ${methodDoc match{
      case None => q"scala.None"
      case Some(s) => q"scala.Some($s)"
    }},
      ${varargs.contains(true)},
      ($baseArgSym: $curCls, ctx: $ctx, $argListSymbol: Map[String, Seq[String]], $extrasSymbol: Seq[String]) =>
        cask.Router.validate(Seq(..$readArgs)) match{
          case cask.Router.Result.Success(List(..$argNames)) =>
            cask.Router.Result.Success(
              ${wrapOutput(methCall)}
            )
          case x: cask.Router.Result.Error => x
        }
    )
    """

    c.internal.transform(res){(t, a) =>
      c.internal.setPos(t, meth.pos)
      a.default(t)
    }
    res
  }

}