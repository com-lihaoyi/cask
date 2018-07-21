package cask
import collection.mutable
object DispatchTrie{
  def construct[T](index: Int, inputs: Seq[(IndexedSeq[String], T)]): DispatchTrie[T] = {
    val continuations = mutable.Map.empty[String, mutable.Buffer[(IndexedSeq[String], T)]]

    val terminals = mutable.Buffer.empty[(IndexedSeq[String], T)]

    for((path, endPoint) <- inputs) {
      if (path.length < index) () // do nothing
      else if (path.length == index) {
        terminals.append(path -> endPoint)
      } else if (path.length > index){
        val buf = continuations.getOrElseUpdate(path(index), mutable.Buffer.empty)
        buf.append(path -> endPoint)
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
    }else{
      DispatchTrie[T](
        current = terminals.headOption.map(_._2),
        children = continuations.map{ case (k, vs) =>
          if (!k.startsWith("::")) (k, construct(index + 1, vs))
          else (k, DispatchTrie(Some(vs.head._2), Map()))
        }.toMap
      )
    }
  }
}

case class DispatchTrie[T](current: Option[T],
                           children: Map[String, DispatchTrie[T]]){
  final def lookup(input: List[String],
                            bindings: Map[String, String])
                            : Option[(T, Map[String, String])] = {
    input match{
      case Nil => current.map(_ -> bindings)
      case head :: rest =>
        if (children.size == 1 && children.keys.head.startsWith("::")){
          children.values.head.lookup(Nil, bindings + (children.keys.head.drop(2) -> input.mkString("/")))
        }else if (children.size == 1 && children.keys.head.startsWith(":")){
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
