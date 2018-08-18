package cask.internal
import collection.mutable
object DispatchTrie{
  def construct[T](index: Int,
                   inputs: Seq[(IndexedSeq[String], T, Boolean)]): DispatchTrie[T] = {
    val continuations = mutable.Map.empty[String, mutable.Buffer[(IndexedSeq[String], T, Boolean)]]

    val terminals = mutable.Buffer.empty[(IndexedSeq[String], T, Boolean)]

    for((path, endPoint, allowSubpath) <- inputs) {
      if (path.length < index) () // do nothing
      else if (path.length == index) {
        terminals.append((path, endPoint, allowSubpath))
      } else if (path.length > index){
        val buf = continuations.getOrElseUpdate(path(index), mutable.Buffer.empty)
        buf.append((path, endPoint, allowSubpath))
      }
    }

    val wildcards = continuations.filter(_._1(0) == ':')
    if (terminals.length > 1){
      throw new Exception(
        "More than one endpoint has the same path: " +
          terminals.map(_._1.map(_.mkString("/"))).mkString(", ")
      )
    } else if(wildcards.size >= 1 && continuations.size > 1) {
      throw new Exception(
        "Routes overlap with wildcards: " +
          (wildcards ++ continuations).flatMap(_._2).map(_._1.mkString("/"))
      )
    }else if (terminals.headOption.exists(_._3) && continuations.size == 1){
      throw new Exception(
        "Routes overlap with subpath capture: " +
          (wildcards ++ continuations).flatMap(_._2).map(_._1.mkString("/"))
      )
    }else{
      DispatchTrie[T](
        current = terminals.headOption.map(x => x._2 -> x._3),
        children = continuations.map{ case (k, vs) => (k, construct(index + 1, vs))}.toMap
      )
    }
  }
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
}
