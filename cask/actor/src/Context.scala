package cask.actor
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, CanAwait, ExecutionContext, Future, Promise}
import scala.util.Try

/**
 * An extended `scala.concurrent.ExecutionContext`; provides the ability to
 * schedule messages to be sent later, and hooks to track the current number of
 * outstanding tasks or log the actor message sends for debugging purporses
 */
trait Context extends ExecutionContext {
  def reportSchedule(): Context.Token = new Context.Token.Simple()

  def reportSchedule(fileName: sourcecode.FileName,
                     line: sourcecode.Line): Context.Token = {
    new Context.Token.Future(fileName, line)
  }

  def reportSchedule(a: Actor[_],
                     msg: Any,
                     fileName: sourcecode.FileName,
                     line: sourcecode.Line): Context.Token = {
    new Context.Token.Send(a, msg, fileName, line)
  }

  def reportRun(a: Actor[_],
                msg: Any,
                token: Context.Token): Unit = ()

  def reportComplete(token: Context.Token): Unit = ()

  def scheduleMsg[T](a: Actor[T], msg: T, time: java.time.Duration)
                    (implicit fileName: sourcecode.FileName,
                     line: sourcecode.Line): Unit

  def future[T](t: => T)
               (implicit fileName: sourcecode.FileName,
                line: sourcecode.Line): Future[T]

  def execute(runnable: Runnable): Unit
}

object Context{
  trait Token
  object Token{
    class Simple extends Token(){
      override def toString = "token@" + Integer.toHexString(hashCode())
    }

    class Future(val fileName: sourcecode.FileName,
                 val line: sourcecode.Line) extends Token(){
      override def toString =
        "token@" + Integer.toHexString(hashCode()) + "@" +
        fileName.value + ":" + line.value
    }

    class Send(val a: Actor[_],
               val msg: Any,
               val fileName: sourcecode.FileName,
               val line: sourcecode.Line) extends Token(){
      override def toString =
        "token@" + Integer.toHexString(hashCode()) + "@" +
        fileName.value + ":" + line.value
    }
  }

  class Simple(ec: ExecutionContext, logEx: Throwable => Unit) extends Context.Impl {
    def executionContext = ec
    def reportFailure(t: Throwable) = logEx(t)
  }

  object Simple{
    implicit val global: Simple = new Simple(scala.concurrent.ExecutionContext.Implicits.global, _.printStackTrace())
  }

  class Test(ec: ExecutionContext = scala.concurrent.ExecutionContext.global,
             logEx: Throwable => Unit = _.printStackTrace()) extends Context.Impl {
    private[this] val active = collection.mutable.Set.empty[Context.Token]
    private[this] var promise = concurrent.Promise.successful[Unit](())

    def executionContext = ec

    def reportFailure(t: Throwable) = logEx(t)

    def handleReportSchedule(token: Context.Token) = synchronized{
      if (active.isEmpty) {
        assert(promise.isCompleted)
        promise = concurrent.Promise[Unit]
      }
      active.add(token)
      token
    }
    override def reportSchedule() = {
      handleReportSchedule(super.reportSchedule())
    }
    override def reportSchedule(fileName: sourcecode.FileName,
                                line: sourcecode.Line): Context.Token = {
      handleReportSchedule(super.reportSchedule(fileName, line))
    }

    override def reportSchedule(a: Actor[_],
                                msg: Any,
                                fileName: sourcecode.FileName,
                                line: sourcecode.Line): Context.Token = {
      handleReportSchedule(super.reportSchedule(a, msg, fileName, line))
    }

    override def reportComplete(token: Context.Token) = this.synchronized{
      assert(active.remove(token))

      if (active.isEmpty) promise.success(())
    }

    def waitForInactivity(timeout: Option[java.time.Duration] = None) = {
      Await.result(
        this.synchronized(promise).future,
        timeout match{
          case None => scala.concurrent.duration.Duration.Inf
          case Some(t) => scala.concurrent.duration.Duration.fromNanos(t.toNanos)
        }
      )
    }
  }

  trait Impl extends Context {
    def executionContext: ExecutionContext

    def execute(runnable: Runnable): Unit = {
      val token = reportSchedule()
      executionContext.execute(new Runnable {
        def run(): Unit = {
          try runnable.run()
          finally reportComplete(token)
        }
      })
    }

    def future[T](t: => T)
                 (implicit fileName: sourcecode.FileName,
                  line: sourcecode.Line): Future[T] = {
      val token = reportSchedule(fileName, line)
      val p = Promise[T]
      executionContext.execute(new Runnable {
        def run(): Unit = {
          p.complete(scala.util.Try(t))
          reportComplete(token)
        }
      })
      p.future
    }

    lazy val scheduler = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactory {
        def newThread(r: Runnable): Thread = {
          val t = new Thread(r, "ActorContext-Scheduler-Thread")
          t.setDaemon(true)
          t
        }
      }
    )

    def scheduleMsg[T](a: Actor[T],
                       msg: T, delay: java.time.Duration)
                      (implicit fileName: sourcecode.FileName,
                       line: sourcecode.Line) = {
      val token = reportSchedule(a, msg, fileName, line)
      scheduler.schedule[Unit](
        () => {
          a.send(msg)(fileName, line)
          reportComplete(token)
        },
        delay.toMillis,
        TimeUnit.MILLISECONDS
      )
    }
  }

}

trait Actor[T]{
  def send(t: T)
          (implicit fileName: sourcecode.FileName,
           line: sourcecode.Line): Unit

  def sendAsync(f: scala.concurrent.Future[T])
               (implicit fileName: sourcecode.FileName,
                line: sourcecode.Line): Unit
}
