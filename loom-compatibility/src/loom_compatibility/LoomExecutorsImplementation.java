package loom_compatibility;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

class LoomExecutorsImplementation implements LoomExecutors {
    static {
        Thread.ofVirtual();
    }

    @Override
    public ExecutorService newThreadPerTaskExecutor(ThreadFactory threadFactory) {
        return java.util.concurrent.Executors.newThreadPerTaskExecutor(threadFactory);
    }

    @Override
    public ExecutorService newVirtualThreadPerTaskExecutor() {
        return java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    }
}
