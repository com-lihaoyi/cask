package cask.router

import scala.reflect.macros.blackbox


class Macros[C <: blackbox.Context](val c: C) {
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
      val (docTrees, remaining) = annotations.partition(_.tpe =:= typeOf[doc])
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
      val argReader = argReaders.lift(argListIndex).getOrElse(q"cask.router.NoOpParser.instanceAny")
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
          cask.router.ArgSig[$annotDeserializeType, $curCls, $docUnwrappedType, $ctx](
            ${arg.name.toString},
            ${docUnwrappedType.toString},
            $docTree,
            $defaultOpt
          )($argReader[$docUnwrappedType])
        """

        val reader = q"""
          cask.router.Runtime.makeReadCall(
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
    cask.router.EntryPoint[$curCls, $ctx](
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
        $argSigsSymbol: scala.Seq[scala.Seq[cask.router.ArgSig[Any, _, _, $ctx]]]
      ) =>
        cask.router.Runtime.validate(Seq(..${readArgs.flatten.toList})).map{
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
