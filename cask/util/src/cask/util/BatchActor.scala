package cask.util

import scala.collection.mutable
import scala.concurrent.ExecutionContext

/**
 * A simple asynchronous actor, allowing safe concurrent asynchronous processing
 * of queued items. `run` handles items in batches, to allow for batch
 * processing optimizations to be used where relevant.
 */
abstract class BatchActor[T]()(implicit ec: ExecutionContext,
                               log: Logger) {
  def run(items: Seq[T]): Unit

  private val queue = new mutable.Queue[T]()
  private var scheduled = false
  def send(t: T): Unit = synchronized{
    queue.enqueue(t)
    if (!scheduled){
      scheduled = true
      ec.execute(() => runWithItems())
    }
  }

  private[this] def runWithItems(): Unit = {
    val items = synchronized(queue.dequeueAll(_ => true))
    try run(items)
    catch{case e: Throwable => log.exception(e)}
    synchronized{
      if (queue.nonEmpty) ec.execute(() => runWithItems())
      else{
        assert(scheduled)
        scheduled = false
      }
    }
  }
}

abstract class StateMachineActor[T]()
                                   (implicit ec: ExecutionContext,
                                    log: Logger) extends BatchActor[T](){
  class State(val run: T => State)
  protected[this] def initialState: State
  protected[this] var state: State = initialState
  def run(items: Seq[T]): Unit = items.foreach{i => state = state.run(i)}
}
