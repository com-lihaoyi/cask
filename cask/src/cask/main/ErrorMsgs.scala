package cask.main


import cask.internal.{Router, Util}
import cask.internal.Util.literalize

object ErrorMsgs {
  def getLeftColWidth(items: Seq[Router.ArgSig[_, _, _,_]]) = {
    items.map(_.name.length + 2) match{
      case Nil => 0
      case x => x.max
    }
  }

  def renderArg[T](base: T,
                   arg: Router.ArgSig[_, T, _, _],
                   leftOffset: Int,
                   wrappedWidth: Int): (String, String) = {
    val suffix = arg.default match{
      case Some(f) => " (default " + f(base) + ")"
      case None => ""
    }
    val docSuffix = arg.doc match{
      case Some(d) => ": " + d
      case None => ""
    }
    val wrapped = Util.softWrap(
      arg.typeString + suffix + docSuffix,
      leftOffset,
      wrappedWidth - leftOffset
    )
    (arg.name, wrapped)
  }

  def formatMainMethodSignature[T](base: T,
                                   main: Router.EntryPoint[T, _],
                                   leftIndent: Int,
                                   leftColWidth: Int) = {
    // +2 for space on right of left col
    val args = main.argSignatures.last.map(as => renderArg(base, as, leftColWidth + leftIndent + 2 + 2, 80))

    val leftIndentStr = " " * leftIndent
    val argStrings =
      for((lhs, rhs) <- args)
        yield {
          val lhsPadded = lhs.padTo(leftColWidth, ' ')
          val rhsPadded = rhs.lines.mkString("\n")
          s"$leftIndentStr  $lhsPadded  $rhsPadded"
        }
    val mainDocSuffix = main.doc match{
      case Some(d) => "\n" + leftIndentStr + Util.softWrap(d, leftIndent, 80)
      case None => ""
    }

    s"""$leftIndentStr${main.name}$mainDocSuffix
       |${argStrings.map(_ + "\n").mkString}""".stripMargin
  }

  def formatInvokeError[T](base: T, route: Router.EntryPoint[T, _], x: Router.Result.Error): String = {
    def expectedMsg = formatMainMethodSignature(base: T, route, 0, 0)

    x match{
      case Router.Result.Error.Exception(x) => Util.stackTraceString(x)
      case Router.Result.Error.MismatchedArguments(missing, unknown) =>
        val missingStr =
          if (missing.isEmpty) ""
          else {
            val chunks =
              for (x <- missing)
                yield x.name + ": " + x.typeString

            val argumentsStr = Util.pluralize("argument", chunks.length)
            s"Missing $argumentsStr: (${chunks.mkString(", ")})\n"
          }


        val unknownStr =
          if (unknown.isEmpty) ""
          else {
            val argumentsStr = Util.pluralize("argument", unknown.length)
            s"Unknown $argumentsStr: " + unknown.map(literalize(_)).mkString(" ") + "\n"
          }


        s"""$missingStr$unknownStr
           |Arguments provided did not match expected signature:
           |
           |$expectedMsg
           |""".stripMargin

      case Router.Result.Error.InvalidArguments(x) =>
        val argumentsStr = Util.pluralize("argument", x.length)
        val thingies = x.map{
          case Router.Result.ParamError.Invalid(p, v, ex) =>
            val literalV = literalize(v)
            val trace = Util.stackTraceString(ex)
            s"${p.name}: ${p.typeString} = $literalV failed to parse with $ex\n$trace"
          case Router.Result.ParamError.DefaultFailed(p, ex) =>
            val trace = Util.stackTraceString(ex)
            s"${p.name}'s default value failed to evaluate with $ex\n$trace"
        }

        s"""The following $argumentsStr failed to parse:
           |
           |${thingies.mkString("\n")}
           |
           |expected signature:
           |
           |$expectedMsg
           |""".stripMargin

    }
  }
}
