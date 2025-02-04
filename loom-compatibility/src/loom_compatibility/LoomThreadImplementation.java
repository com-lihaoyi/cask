package loom_compatibility;

import java.util.concurrent.ThreadFactory;

class LoomThreadImplementation implements LoomThread {

    static {
        Thread.ofVirtual();
    }

    @Override
    public Builder.OfPlatform ofPlatform() {
        return new OfPlatformImplementation(Thread.ofPlatform());
    }

    @Override
    public Builder.OfVirtual ofVirtual() {
        return new OfVirtualImplementation(Thread.ofVirtual());
    }

    @Override
    public Thread startVirtualThread(Runnable task) {
        return Thread.startVirtualThread(task);
    }

    @Override
    public boolean isVirtual(Thread thread) {
        return thread.isVirtual();
    }

    private static class OfPlatformImplementation implements Builder.OfPlatform {

        Thread.Builder.OfPlatform builder;

        OfPlatformImplementation(Thread.Builder.OfPlatform builder) {
            this.builder = builder;
        }

        @Override
        public OfPlatform name(String name) {
            builder = builder.name(name);
            return this;
        }

        @Override
        public OfPlatform name(String prefix, long start) {
            builder = builder.name(prefix, start);
            return this;
        }

        @Override
        public OfPlatform inheritInheritableThreadLocals(boolean inherit) {
            builder = builder.inheritInheritableThreadLocals(inherit);
            return this;
        }

        @Override
        public OfPlatform uncaughtExceptionHandler(Thread.UncaughtExceptionHandler ueh) {
            builder = builder.uncaughtExceptionHandler(ueh);
            return this;
        }

        @Override
        public OfPlatform group(ThreadGroup group) {
            builder = builder.group(group);
            return this;
        }

        @Override
        public OfPlatform daemon(boolean on) {
            builder = builder.daemon(on);
            return this;
        }

        @Override
        public OfPlatform priority(int priority) {
            builder = builder.priority(priority);
            return this;
        }

        @Override
        public OfPlatform stackSize(long stackSize) {
            builder = builder.stackSize(stackSize);
            return this;
        }

        @Override
        public Thread unstarted(Runnable task) {
            return builder.unstarted(task);
        }

        @Override
        public Thread start(Runnable task) {
            return builder.start(task);
        }

        @Override
        public ThreadFactory factory() {
            return builder.factory();
        }

        @Override
        public OfPlatform daemon() {
            builder = builder.daemon();
            return this;
        }
    }

    private static class OfVirtualImplementation implements Builder.OfVirtual {

        Thread.Builder.OfVirtual builder;

        OfVirtualImplementation(Thread.Builder.OfVirtual builder) {
            this.builder = builder;
        }

        @Override
        public OfVirtual inheritInheritableThreadLocals(boolean inherit) {
            builder = builder.inheritInheritableThreadLocals(inherit);
            return this;
        }

        @Override
        public OfVirtual name(String name) {
            builder = builder.name(name);
            return this;
        }

        @Override
        public OfVirtual name(String prefix, long start) {
            builder = builder.name(prefix, start);
            return this;
        }

        @Override
        public OfVirtual uncaughtExceptionHandler(Thread.UncaughtExceptionHandler ueh) {
            builder = builder.uncaughtExceptionHandler(ueh);
            return this;
        }

        @Override
        public Thread unstarted(Runnable task) {
            return builder.unstarted(task);
        }

        @Override
        public Thread start(Runnable task) {
            return builder.start(task);
        }

        @Override
        public ThreadFactory factory() {
            return builder.factory();
        }
    }
}
