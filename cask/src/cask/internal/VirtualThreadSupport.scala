package cask.internal

import java.lang.invoke.{MethodHandles, MethodType}
import java.util.concurrent.ThreadFactory

private[cask] object VirtualThreadSupport {

  /**
   * Returns if the current Runtime supports virtual threads.
   */
  lazy val isVirtualThreadSupported: Boolean = create("testIfSupported") ne null

  /**
   * Create a virtual thread factory, returns null when failed.
   */
  def create(prefix: String): ThreadFactory =
    try {
      val builderClass = ClassLoader.getSystemClassLoader.loadClass("java.lang.Thread$Builder")
      val ofVirtualClass = ClassLoader.getSystemClassLoader.loadClass("java.lang.Thread$Builder$OfVirtual")
      val lookup = MethodHandles.lookup
      val ofVirtualMethod = lookup.findStatic(classOf[Thread], "ofVirtual", MethodType.methodType(ofVirtualClass))
      var builder = ofVirtualMethod.invoke()
      val nameMethod = lookup.findVirtual(ofVirtualClass, "name",
        MethodType.methodType(ofVirtualClass, classOf[String], classOf[Long]))
      val factoryMethod = lookup.findVirtual(builderClass, "factory", MethodType.methodType(classOf[ThreadFactory]))
      builder = nameMethod.invoke(builder, prefix + "-virtual-thread-", 0L)
      factoryMethod.invoke(builder).asInstanceOf[ThreadFactory]
    } catch {
      case _: Throwable => null
    }
}
