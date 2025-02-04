package loom_compatibility;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public interface LoomExecutors {

    static LoomExecutors load() throws LoomUnavailable {
        try {
            return new LoomExecutorsImplementation();
        } catch (LinkageError e) {
            throw new LoomUnavailable(e);
        }
    }

    /**
     * Creates an Executor that starts a new Thread for each task.
     * The number of threads created by the Executor is unbounded.
     *
     * <p> Invoking {@link Future#cancel(boolean) cancel(true)} on a {@link
     * Future Future} representing the pending result of a task submitted to
     * the Executor will {@link Thread#interrupt() interrupt} the thread
     * executing the task.
     *
     * @param threadFactory the factory to use when creating new threads
     * @return a new executor that creates a new Thread for each task
     * @throws NullPointerException if threadFactory is null
     * @since 21
     */
    public ExecutorService newThreadPerTaskExecutor(ThreadFactory threadFactory);

    /**
     * Creates an Executor that starts a new virtual Thread for each task.
     * The number of threads created by the Executor is unbounded.
     *
     * <p> This method is equivalent to invoking
     * {@link #newThreadPerTaskExecutor(ThreadFactory)} with a thread factory
     * that creates virtual threads.
     *
     * @return a new executor that creates a new virtual Thread for each task
     * @since 21
     */
    public ExecutorService newVirtualThreadPerTaskExecutor();
}
