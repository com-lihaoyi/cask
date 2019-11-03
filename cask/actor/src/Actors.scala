package cask.actor
import collection.mutable

abstract class BaseActor[T]()(implicit ac: Context) extends Actor[T]{
  private val queue = new mutable.Queue[(T, Context.Token)]()

  private var scheduled = false

  def send(t: T)
          (implicit fileName: sourcecode.FileName,
           line: sourcecode.Line): Unit = synchronized{
    val token = ac.reportSchedule(this, t, fileName, line)
    queue.enqueue((t, token))
    if (!scheduled){
      scheduled = true
      ac.execute(() => runWithItems())
    }
  }
  def sendAsync(f: scala.concurrent.Future[T])
               (implicit fileName: sourcecode.FileName,
                line: sourcecode.Line) = {
    f.onComplete{
      case scala.util.Success(v) => this.send(v)
      case scala.util.Failure(e) => ac.reportFailure(e)
    }
  }

  def runBatch0(msgs: Seq[(T, Context.Token)]): Unit
  private[this] def runWithItems(): Unit = {
    val msgs = synchronized(queue.dequeueAll(_ => true))

    runBatch0(msgs)

    synchronized{
      if (queue.nonEmpty) ac.execute(() => runWithItems())
      else{
        assert(scheduled)
        scheduled = false
      }
    }
  }
}

abstract class BatchActor[T]()(implicit ac: Context) extends BaseActor[T]{
  def runBatch(msgs: Seq[T]): Unit
  def runBatch0(msgs: Seq[(T, Context.Token)]): Unit = {
    try {
      msgs.foreach{case (m, token) => ac.reportRun(this, m, token)}
      runBatch(msgs.map(_._1))
    }
    catch{case e: Throwable => ac.reportFailure(e)}
    finally msgs.foreach{case (m, token) => ac.reportComplete(token)}

  }
}

abstract class SimpleActor[T]()(implicit ac: Context) extends BaseActor[T]{
  def run(msg: T): Unit
  override def runBatch0(msgs: Seq[(T, Context.Token)]): Unit = {
    for((msg, token) <- msgs) {
      try {
        ac.reportRun(this, msg, token)
        run(msg)
      }
      catch{case e: Throwable => ac.reportFailure(e)}
      finally ac.reportComplete(token)
    }
  }
}

abstract class StateMachineActor[T]()(implicit ac: Context) extends SimpleActor[T]() {
  class State(val run: T => State)
  protected[this] def initialState: State
  protected[this] var state: State = initialState
  def run(msg: T): Unit = {
    state = state.run(msg)
  }
}