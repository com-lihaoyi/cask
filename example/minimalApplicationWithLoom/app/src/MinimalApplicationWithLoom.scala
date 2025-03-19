package app

import cask.main.Main

import java.lang.management.{ManagementFactory, RuntimeMXBean}
import java.util.concurrent.{ExecutorService, Executors}

// run benchmark with : ./mill benchmark.runBenchmark
object MinimalApplicationWithLoom extends cask.MainRoutes {
  // Print Java version
  private val javaVersion: String = System.getProperty("java.version")
  println("Java Version: " + javaVersion)

  // Print JVM arguments// Print JVM arguments
  private val runtimeMxBean: RuntimeMXBean = ManagementFactory.getRuntimeMXBean
  private val jvmArguments = runtimeMxBean.getInputArguments
  println("JVM Arguments:")

  jvmArguments.forEach((arg: String) => println(arg))

  println(
    Main.VIRTUAL_THREAD_ENABLED + " :" + System.getProperty(
      Main.VIRTUAL_THREAD_ENABLED
    )
  )

  // Use the same underlying executor for both virtual and non-virtual threads
  private val executor = Executors.newFixedThreadPool(4)

  // TO USE LOOM:
  // 1. JDK 21 or later is needed.
  // 2. add VM option: --add-opens java.base/java.lang=ALL-UNNAMED
  // 3. set system property: cask.virtual-threads.enabled=true
  // 4. NOTE: `java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor` is using the shared
  //   ForkJoinPool in VirtualThread. If you want to use a separate ForkJoinPool, you can create
  //   a new ForkJoinPool instance and pass it to `createVirtualThreadExecutor` method.

  override protected def handlerExecutor(): Option[ExecutorService] = {
    super.handlerExecutor().orElse(Some(executor))
  }

  /** With curl: curl -X GET http://localhost:8080/ you wil see something like:
    * Hello World! from
    * thread:VirtualThread[#63,cask-handler-executor-virtual-thread-10]/runnable@ForkJoinPool-1-worker-1%
    */
  @cask.get("/")
  def hello() = {
    Thread.sleep(100) // simulate some blocking work
    "Hello World!"
  }

  @cask.post("/do-thing")
  def doThing(request: cask.Request) = {
    request.text().reverse
  }

  initialize()
}
