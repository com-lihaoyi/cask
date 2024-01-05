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
      else if (path.length == index) terminals.append((path, endPoint, allowSubpath))
      else if (path.length > index){
        val buf = continuations.getOrElseUpdate(path(index), mutable.Buffer.empty)
        buf.append((path, endPoint, allowSubpath))
      }
    }

    validateGroups(inputs, terminals, continuations)(validationGroups)

    DispatchTrie[T](
      current = terminals.headOption.map(x => x._2 -> x._3),
      children = continuations
        .map{ case (k, vs) => (k, construct(index + 1, vs)(validationGroups))}
        .toMap
    )
  }

  def validateGroups[T, V](inputs: collection.Seq[(collection.IndexedSeq[String], T, Boolean)],
                           terminals0: collection.Seq[(collection.IndexedSeq[String], T, Boolean)],
                           continuations0: collection.Map[String, collection.Seq[(collection.IndexedSeq[String], T, Boolean)]])
                          (validationGroups: T => Seq[V]) = {
    for (group <- inputs.flatMap(t => validationGroups(t._2)).distinct) {
      val terminals = terminals0.flatMap { case (path, v, allowSubpath) =>
        validationGroups(v)
          .filter(_ == group)
          .map { group => (path, v, allowSubpath, group) }
      }

      val continuations = continuations0
        .map { case (k, vs) =>
          k -> vs.flatMap { case (path, v, allowSubpath) =>
            validationGroups(v)
              .filter(_ == group)
              .map { group => (path, v, allowSubpath, group) }
          }
        }
        .filter(_._2.nonEmpty)

      val wildcards = continuations.filter(_._1(0) == ':')

      def render(values: collection.Seq[(collection.IndexedSeq[String], T, Boolean, V)]) = values
        .map { case (path, v, allowSubpath, group) => s"$group${renderPath(path)}" }
        .mkString(", ")

      def renderTerminals = render(terminals)
      def renderContinuations = render(continuations.toSeq.flatMap(_._2))

      if (terminals.length > 1) {
        throw new Exception(
          s"More than one endpoint has the same path: $renderTerminals"
        )
      }

      if (wildcards.size >= 1 && continuations.size > 1) {
        throw new Exception(
          s"Routes overlap with wildcards: $renderContinuations"
        )
      }

      if (terminals.headOption.exists(_._3) && continuations.size == 1) {
        throw new Exception(
          s"Routes overlap with subpath capture: $renderTerminals, $renderContinuations"
        )
      }
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
  */
case class DispatchTrie[T](current: Option[(T, Boolean)],
                           children: Map[String, DispatchTrie[T]]){
  final def lookup(remainingInput: List[String],
                   bindings: Map[String, String])
  : Option[(T, Map[String, String], Seq[String])] = {
    remainingInput match{
      case Nil =>
        current.map(x => (x._1, bindings, Nil))
      case head :: rest if current.exists(_._2) =>
        current.map(x => (x._1, bindings, head :: rest))
      case head :: rest =>
        if (children.size == 1 && children.keys.head.startsWith(":")){
          children.values.head.lookup(rest, bindings + (children.keys.head.drop(1) -> head))
        }else{
          children.get(head) match{
            case None => None
            case Some(continuation) => continuation.lookup(rest, bindings)
          }
        }

    }
  }

  def map[V](f: T => V): DispatchTrie[V] = DispatchTrie(
    current.map{case (t, v) => (f(t), v)},
    children.map { case (k, v) => (k, v.map(f))}
  )
}
