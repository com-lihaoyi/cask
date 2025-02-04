package loom_compatibility;

import java.util.concurrent.ThreadFactory;

public interface LoomThread {

    static LoomThread load() throws LoomUnavailable {
        try {
            return new LoomThreadImplementation();
        } catch (LinkageError e) {
            throw new LoomUnavailable(e);
        }
    }

    /**
     * Returns a builder for creating a platform {@code Thread} or {@code ThreadFactory}
     * that creates platform threads.
     *
     * <p> <a id="ofplatform-security"><b>Interaction with security manager when
     * creating platform threads</b></a>
     * <p> Creating a platform thread when there is a security manager set will
     * invoke the security manager's {@link SecurityManager#checkAccess(ThreadGroup)
     * checkAccess(ThreadGroup)} method with the thread's thread group.
     * If the thread group has not been set with the {@link
     * Thread.Builder.OfPlatform#group(ThreadGroup) OfPlatform.group} method then the
     * security manager's {@link SecurityManager#getThreadGroup() getThreadGroup}
     * method will be invoked first to select the thread group. If the security
     * manager {@code getThreadGroup} method returns {@code null} then the thread
     * group of the constructing thread is used.
     *
     * @return A builder for creating {@code Thread} or {@code ThreadFactory} objects.
     * The following are examples using the builder:
     * {@snippet :
     *   // Start a daemon thread to run a task
     *   Thread thread = Thread.ofPlatform().daemon().start(runnable);
     *
     *   // Create an unstarted thread with name "duke", its start() method
     *   // must be invoked to schedule it to execute.
     *   Thread thread = Thread.ofPlatform().name("duke").unstarted(runnable);
     *
     *   // A ThreadFactory that creates daemon threads named "worker-0", "worker-1", ...
     *   ThreadFactory factory = Thread.ofPlatform().daemon().name("worker-", 0).factory();
     *}
     * @since 21
     */
    Builder.OfPlatform ofPlatform();

    /**
     * Returns a builder for creating a virtual {@code Thread} or {@code ThreadFactory}
     * that creates virtual threads.
     *
     * @return A builder for creating {@code Thread} or {@code ThreadFactory} objects.
     * The following are examples using the builder:
     * {@snippet :
     *   // Start a virtual thread to run a task.
     *   Thread thread = Thread.ofVirtual().start(runnable);
     *
     *   // A ThreadFactory that creates virtual threads
     *   ThreadFactory factory = Thread.ofVirtual().factory();
     *}
     * @since 21
     */
    Builder.OfVirtual ofVirtual();

    /**
     * A builder for {@link Thread} and {@link ThreadFactory} objects.
     *
     * <p> {@code Builder} defines methods to set {@code Thread} properties such
     * as the thread {@link #name(String) name}. This includes properties that would
     * otherwise be <a href="Thread.html#inheritance">inherited</a>. Once set, a
     * {@code Thread} or {@code ThreadFactory} is created with the following methods:
     *
     * <ul>
     *     <li> The {@linkplain #unstarted(Runnable) unstarted} method creates a new
     *          <em>unstarted</em> {@code Thread} to run a task. The {@code Thread}'s
     *          {@link Thread#start() start} method must be invoked to schedule the
     *          thread to execute.
     *     <li> The {@linkplain #start(Runnable) start} method creates a new {@code
     *          Thread} to run a task and schedules the thread to execute.
     *     <li> The {@linkplain #factory() factory} method creates a {@code ThreadFactory}.
     * </ul>
     *
     * <p> A {@code Thread.Builder} is not thread safe. The {@code ThreadFactory}
     * returned by the builder's {@code factory()} method is thread safe.
     *
     * <p> Unless otherwise specified, passing a null argument to a method in
     * this interface causes a {@code NullPointerException} to be thrown.
     *
     * @see Thread#ofPlatform()
     * @see Thread#ofVirtual()
     * @since 21
     */
    interface Builder {

        /**
         * Sets the thread name.
         *
         * @param name thread name
         * @return this builder
         */
        Builder name(String name);

        /**
         * Sets the thread name to be the concatenation of a string prefix and
         * the string representation of a counter value. The counter's initial
         * value is {@code start}. It is incremented after a {@code Thread} is
         * created with this builder so that the next thread is named with
         * the new counter value. A {@code ThreadFactory} created with this
         * builder is seeded with the current value of the counter. The {@code
         * ThreadFactory} increments its copy of the counter after {@link
         * ThreadFactory#newThread(Runnable) newThread} is used to create a
         * {@code Thread}.
         *
         * @param prefix thread name prefix
         * @param start  the starting value of the counter
         * @return this builder
         * @throws IllegalArgumentException if start is negative
         * The following example creates a builder that is invoked twice to start
         * two threads named "{@code worker-0}" and "{@code worker-1}".
         * {@snippet :
         *   Thread.Builder builder = Thread.ofPlatform().name("worker-", 0);
         *   Thread t1 = builder.start(task1);   // name "worker-0"
         *   Thread t2 = builder.start(task2);   // name "worker-1"
         *}
         */
        Builder name(String prefix, long start);

        /**
         * Sets whether the thread inherits the initial values of {@linkplain
         * InheritableThreadLocal inheritable-thread-local} variables from the
         * constructing thread. The default is to inherit.
         *
         * @param inherit {@code true} to inherit, {@code false} to not inherit
         * @return this builder
         */
        Builder inheritInheritableThreadLocals(boolean inherit);

        /**
         * Sets the uncaught exception handler.
         *
         * @param ueh uncaught exception handler
         * @return this builder
         */
        Builder uncaughtExceptionHandler(Thread.UncaughtExceptionHandler ueh);

        /**
         * Creates a new {@code Thread} from the current state of the builder to
         * run the given task. The {@code Thread}'s {@link Thread#start() start}
         * method must be invoked to schedule the thread to execute.
         *
         * @param task the object to run when the thread executes
         * @return a new unstarted Thread
         * @throws SecurityException if denied by the security manager
         *                           (See <a href="Thread.html#ofplatform-security">Interaction with
         *                           security manager when creating platform threads</a>)
         * @see <a href="Thread.html#inheritance">Inheritance when creating threads</a>
         */
        Thread unstarted(Runnable task);

        /**
         * Creates a new {@code Thread} from the current state of the builder and
         * schedules it to execute.
         *
         * @param task the object to run when the thread executes
         * @return a new started Thread
         * @throws SecurityException if denied by the security manager
         *                           (See <a href="Thread.html#ofplatform-security">Interaction with
         *                           security manager when creating platform threads</a>)
         * @see <a href="Thread.html#inheritance">Inheritance when creating threads</a>
         */
        Thread start(Runnable task);

        /**
         * Returns a {@code ThreadFactory} to create threads from the current
         * state of the builder. The returned thread factory is safe for use by
         * multiple concurrent threads.
         *
         * @return a thread factory to create threads
         */
        ThreadFactory factory();

        /**
         * A builder for creating a platform {@link Thread} or {@link ThreadFactory}
         * that creates platform threads.
         *
         * <p> Unless otherwise specified, passing a null argument to a method in
         * this interface causes a {@code NullPointerException} to be thrown.
         *
         * @see Thread#ofPlatform()
         * @since 21
         */
        interface OfPlatform extends Builder {

            @Override
            OfPlatform name(String name);

            /**
             * @throws IllegalArgumentException {@inheritDoc}
             */
            @Override
            OfPlatform name(String prefix, long start);

            @Override
            OfPlatform inheritInheritableThreadLocals(boolean inherit);

            @Override
            OfPlatform uncaughtExceptionHandler(Thread.UncaughtExceptionHandler ueh);

            /**
             * Sets the thread group.
             *
             * @param group the thread group
             * @return this builder
             */
            OfPlatform group(ThreadGroup group);

            /**
             * Sets the daemon status.
             *
             * @param on {@code true} to create daemon threads
             * @return this builder
             */
            OfPlatform daemon(boolean on);

            /**
             * Sets the daemon status to {@code true}.
             *
             * @return this builder
             * The default implementation invokes {@linkplain #daemon(boolean)} with
             * a value of {@code true}.
             */
            default OfPlatform daemon() {
                return daemon(true);
            }

            /**
             * Sets the thread priority.
             *
             * @param priority priority
             * @return this builder
             * @throws IllegalArgumentException if the priority is less than
             *                                  {@link Thread#MIN_PRIORITY} or greater than {@link Thread#MAX_PRIORITY}
             */
            OfPlatform priority(int priority);

            /**
             * Sets the desired stack size.
             *
             * <p> The stack size is the approximate number of bytes of address space
             * that the Java virtual machine is to allocate for the thread's stack. The
             * effect is highly platform dependent and the Java virtual machine is free
             * to treat the {@code stackSize} parameter as a "suggestion". If the value
             * is unreasonably low for the platform then a platform specific minimum
             * may be used. If the value is unreasonably high then a platform specific
             * maximum may be used. A value of zero is always ignored.
             *
             * @param stackSize the desired stack size
             * @return this builder
             * @throws IllegalArgumentException if the stack size is negative
             */
            OfPlatform stackSize(long stackSize);
        }

        /**
         * A builder for creating a virtual {@link Thread} or {@link ThreadFactory}
         * that creates virtual threads.
         *
         * <p> Unless otherwise specified, passing a null argument to a method in
         * this interface causes a {@code NullPointerException} to be thrown.
         *
         * @see Thread#ofVirtual()
         * @since 21
         */
        interface OfVirtual extends Builder {

            @Override
            OfVirtual name(String name);

            /**
             * @throws IllegalArgumentException {@inheritDoc}
             */
            @Override
            OfVirtual name(String prefix, long start);

            @Override
            OfVirtual inheritInheritableThreadLocals(boolean inherit);

            @Override
            OfVirtual uncaughtExceptionHandler(Thread.UncaughtExceptionHandler ueh);
        }
    }

    /**
     * Creates a virtual thread to execute a task and schedules it to execute.
     *
     * <p> This method is equivalent to:
     * <pre>{@code Thread.ofVirtual().start(task); }</pre>
     *
     * @param task the object to run when the thread executes
     * @return a new, and started, virtual thread
     * @see <a href="#inheritance">Inheritance when creating threads</a>
     * @since 21
     */
    Thread startVirtualThread(Runnable task);

    /**
     * Returns {@code true} if this thread is a virtual thread. A virtual thread
     * is scheduled by the Java virtual machine rather than the operating system.
     *
     * @return {@code true} if this thread is a virtual thread
     * @since 21
     */
    boolean isVirtual(Thread thread);
}
