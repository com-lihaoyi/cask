package cask.internal

import java.io.{InputStream, PrintWriter, StringWriter}
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable
import java.io.OutputStream
import java.lang.invoke.{MethodHandles, MethodType}
import java.util.concurrent.{Executor, ExecutorService, ForkJoinPool, ThreadFactory}
import scala.annotation.switch
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try
import scala.util.control.NonFatal

object Util {
  private val lookup = MethodHandles.lookup()

  import cask.util.Logger.Console.globalLogger

  /**
   * Create a virtual thread executor with the given executor as the scheduler.
   * */
  def createVirtualThreadExecutor(executor: Executor): Option[ExecutorService] = {
    (for {
      factory <- Try(createVirtualThreadFactory("cask-handler-executor", executor))
      executor <- Try(createNewThreadPerTaskExecutor(factory))
    } yield executor).toOption
  }

  /**
   * Create a default cask virtual thread executor if possible.
   * */
  def createDefaultCaskVirtualThreadExecutor: Option[ExecutorService] = {
    for {
      scheduler <- getDefaultVirtualThreadScheduler
      executor <- createVirtualThreadExecutor(scheduler)
    } yield executor
  }

  /**
   * Try to get the default virtual thread scheduler, or null if not supported.
   * */
  def getDefaultVirtualThreadScheduler: Option[ForkJoinPool] = {
    try {
      val virtualThreadClass = Class.forName("java.lang.VirtualThread")
      val privateLookup = MethodHandles.privateLookupIn(virtualThreadClass, lookup)
      val defaultSchedulerField = privateLookup.findStaticVarHandle(virtualThreadClass, "DEFAULT_SCHEDULER", classOf[ForkJoinPool])
      Option(defaultSchedulerField.get().asInstanceOf[ForkJoinPool])
    } catch {
      case NonFatal(e) =>
        //--add-opens java.base/java.lang=ALL-UNNAMED
        globalLogger.exception(e)
        None
    }
  }

  def createNewThreadPerTaskExecutor(threadFactory: ThreadFactory): ExecutorService = {
    try {
      val executorsClazz = ClassLoader.getSystemClassLoader.loadClass("java.util.concurrent.Executors")
      val newThreadPerTaskExecutorMethod = lookup.findStatic(
        executorsClazz,
        "newThreadPerTaskExecutor",
        MethodType.methodType(classOf[ExecutorService], classOf[ThreadFactory]))
      newThreadPerTaskExecutorMethod.invoke(threadFactory)
        .asInstanceOf[ExecutorService]
    } catch {
      case NonFatal(e) =>
        globalLogger.exception(e)
        throw new UnsupportedOperationException("Failed to create newThreadPerTaskExecutor.", e)
    }
  }

  /**
   * Create a virtual thread factory with a executor, the executor will be used as the scheduler of
   * virtual thread.
   *
   * The executor should run task on platform threads.
   *
   * returns null if not supported.
   */
  def createVirtualThreadFactory(prefix: String,
                                 executor: Executor): ThreadFactory =
    try {
      val builderClass = ClassLoader.getSystemClassLoader.loadClass("java.lang.Thread$Builder")
      val ofVirtualClass = ClassLoader.getSystemClassLoader.loadClass("java.lang.Thread$Builder$OfVirtual")
      val ofVirtualMethod = lookup.findStatic(classOf[Thread], "ofVirtual", MethodType.methodType(ofVirtualClass))
      var builder = ofVirtualMethod.invoke()
      if (executor != null) {
        val clazz = builder.getClass
        val privateLookup = MethodHandles.privateLookupIn(
          clazz,
          lookup
        )
        val schedulerFieldSetter = privateLookup
          .findSetter(clazz, "scheduler", classOf[Executor])
        schedulerFieldSetter.invoke(builder, executor)
      }
      val nameMethod = lookup.findVirtual(ofVirtualClass, "name",
        MethodType.methodType(ofVirtualClass, classOf[String], classOf[Long]))
      val factoryMethod = lookup.findVirtual(builderClass, "factory", MethodType.methodType(classOf[ThreadFactory]))
      builder = nameMethod.invoke(builder, prefix + "-virtual-thread-", 0L)
      factoryMethod.invoke(builder).asInstanceOf[ThreadFactory]
    } catch {
      case NonFatal(e) =>
        globalLogger.exception(e)
        //--add-opens java.base/java.lang=ALL-UNNAMED
        throw new UnsupportedOperationException("Failed to create virtual thread factory.", e)
    }

  def firstFutureOf[T](futures: Seq[Future[T]])(implicit ec: ExecutionContext) = {
    val p = Promise[T]
    futures.foreach(_.foreach(p.trySuccess))
    p.future
  }

  /**
   * Convert a string to a C&P-able literal. Basically
   * copied verbatim from the uPickle source code.
   */
  def literalize(s: IndexedSeq[Char], unicode: Boolean = true) = {
    val sb = new StringBuilder
    sb.append('"')
    var i = 0
    val len = s.length
    while (i < len) {
      (s(i): @switch) match {
        case '"' => sb.append("\\\"")
        case '\\' => sb.append("\\\\")
        case '\b' => sb.append("\\b")
        case '\f' => sb.append("\\f")
        case '\n' => sb.append("\\n")
        case '\r' => sb.append("\\r")
        case '\t' => sb.append("\\t")
        case c =>
          if (c < ' ' || (c > '~' && unicode)) sb.append("\\u%04x" format c.toInt)
          else sb.append(c)
      }
      i += 1
    }
    sb.append('"')

    sb.result()
  }

  def transferTo(in: InputStream, out: OutputStream) = {
    val buffer = new Array[Byte](8192)

    while ( {
      in.read(buffer) match {
        case -1 => false
        case n =>
          out.write(buffer, 0, n)
          true
      }
    }) ()
  }

  def pluralize(s: String, n: Int) = {
    if (n == 1) s else s + "s"
  }

  /**
   * Splits a string into path segments; automatically removes all
   * leading/trailing slashes, and ignores empty path segments.
   *
   * Written imperatively for performance since it's used all over the place.
   */
  def splitPath(p: String): collection.IndexedSeq[String] = {
    val pLength = p.length
    var i = 0
    while (i < pLength && p(i) == '/') i += 1
    var segmentStart = i
    val out = mutable.ArrayBuffer.empty[String]

    def complete() = {
      if (i != segmentStart) {
        val s = p.substring(segmentStart, i)
        out += s
      }
      segmentStart = i + 1
    }

    while (i < pLength) {
      if (p(i) == '/') complete()
      i += 1
    }
    complete()
    out
  }

  def stackTraceString(e: Throwable) = {
    val trace = new StringWriter()
    val pw = new PrintWriter(trace)
    e.printStackTrace(pw)
    pw.flush()
    trace.toString
  }

  def softWrap(s: String, leftOffset: Int, maxWidth: Int) = {
    val oneLine = s.linesIterator.mkString(" ").split(' ')

    lazy val indent = " " * leftOffset

    val output = new StringBuilder(oneLine.head)
    var currentLineWidth = oneLine.head.length
    for (chunk <- oneLine.tail) {
      val addedWidth = currentLineWidth + chunk.length + 1
      if (addedWidth > maxWidth) {
        output.append("\n" + indent)
        output.append(chunk)
        currentLineWidth = chunk.length
      } else {
        currentLineWidth = addedWidth
        output.append(' ')
        output.append(chunk)
      }
    }
    output.mkString
  }

  def sequenceEither[A, B, M[X] <: TraversableOnce[X]](in: M[Either[A, B]])(
    implicit cbf: CanBuildFrom[M[Either[A, B]], B, M[B]]): Either[A, M[B]] = {
    in.foldLeft[Either[A, mutable.Builder[B, M[B]]]](Right(cbf(in))) {
        case (acc, el) =>
          for (a <- acc; e <- el) yield a += e
      }
      .map(_.result())
  }
}
