package cask.internal
import collection.mutable
object DispatchTrie{
  def construct[T, V](index: Int,
                      inputs: collection.Seq[(collection.IndexedSeq[String], T, Boolean)])
                     (validationGroups: T => Seq[V]): DispatchTrie[T] = {
    val continuations = mutable.Map.empty[String, mutable.Buffer[(collection.IndexedSeq[String], T, Boolean)]]

    val terminals = mutable.Buffer.empty[(collection.IndexedSeq[String], T, Boolean)]

    for((path, endPoint, allowSubpath) <- inputs) {
      if (path.length < index) () // do nothing
      else if (path.length == index) {
        terminals.append((path, endPoint, allowSubpath))
      } else if (path.length > index){
        val buf = continuations.getOrElseUpdate(path(index), mutable.Buffer.empty)
        buf.append((path, endPoint, allowSubpath))
      }
    }

    for(group <- inputs.flatMap(t => validationGroups(t._2)).distinct) {
      val groupTerminals = terminals.flatMap{case (path, v, allowSubpath) =>
        validationGroups(v)
          .filter(_ == group)
          .map{group => (path, v, allowSubpath, group)}
      }

      val groupContinuations = continuations
        .map { case (k, vs) =>
          k -> vs.flatMap { case (path, v, allowSubpath) =>
            validationGroups(v)
              .filter(_ == group)
              .map { group => (path, v, allowSubpath, group) }
          }
        }
        .filter(_._2.nonEmpty)

      validateGroup(groupTerminals, groupContinuations)
    }

    val dynamicChildren = continuations.filter(_._1.startsWith(":"))
      .flatMap(_._2).toIndexedSeq

    DispatchTrie[T](
      current = terminals.headOption
        .map{ case (path, value, capturesSubpath) =>
          val argNames = path.filter(_.startsWith(":")).map(_.drop(1)).toVector
          (value, capturesSubpath, argNames)
        },
      staticChildren = continuations
        .filter(!_._1.startsWith(":"))
        .map{ case (k, vs) => (k, construct(index + 1, vs)(validationGroups))}
        .toMap,
      dynamicChildren = if (dynamicChildren.isEmpty) None else Some(construct(index + 1, dynamicChildren)(validationGroups))
    )
  }

  def validateGroup[T, V](terminals: collection.Seq[(collection.Seq[String], T, Boolean, V)],
                          continuations: mutable.Map[String, mutable.Buffer[(collection.IndexedSeq[String], T, Boolean, V)]]) = {

    def renderTerminals = terminals
      .map{case (path, v, allowSubpath, group) => s"$group${renderPath(path)}"}
      .mkString(", ")

    def renderContinuations = continuations.toSeq
        .flatMap(_._2)
        .map{case (path, v, allowSubpath, group) => s"$group${renderPath(path)}"}
        .mkString(", ")

    if (terminals.length > 1) {
      throw new Exception(
        s"More than one endpoint has the same path: $renderTerminals"
      )
    }

    if (terminals.headOption.exists(_._3) && continuations.size == 1) {
      throw new Exception(
        s"Routes overlap with subpath capture: $renderTerminals, $renderContinuations"
      )
    }
  }

  def renderPath(p: collection.Seq[String]) = " /" + p.mkString("/")
}

/**
  * A simple Trie that can be compiled from a list of endpoints, to allow
  * endpoint lookup in O(length-of-request-path) time. Lookup returns the
  * [[T]] this trie contains, as well as a map of bound wildcards (path
  * segments starting with `:`) and any remaining un-used path segments
  * (only when `current._2 == true`, indicating this route allows trailing
  * segments)
  * current = (value, captures subpaths, argument names)
  */
case class DispatchTrie[T](
  current: Option[(T, Boolean, Vector[String])],
  staticChildren: Map[String, DispatchTrie[T]],
  dynamicChildren: Option[DispatchTrie[T]]
) {

  final def lookup(remainingInput: List[String],
                   bindings: Vector[String])
  : Option[(T, Map[String, String], Seq[String])] = {
    remainingInput match {
      case Nil =>
        current.map(x => (x._1, x._3.zip(bindings).toMap, Nil))
      case head :: rest if current.exists(_._2) =>
        current.map(x => (x._1, x._3.zip(bindings).toMap, head :: rest))
      case head :: rest =>
        staticChildren.get(head) match {
          case Some(continuation) => continuation.lookup(rest, bindings)
          case None =>
            dynamicChildren match {
              case Some(continuation) => continuation.lookup(rest, bindings :+ head)
              case None => None
            }
        }
    }
  }

  def map[V](f: T => V): DispatchTrie[V] = DispatchTrie(
    current.map{case (t, v, a) => (f(t), v, a)},
    staticChildren.map { case (k, v) => (k, v.map(f))},
    dynamicChildren.map { case v => v.map(f)},
  )
}
