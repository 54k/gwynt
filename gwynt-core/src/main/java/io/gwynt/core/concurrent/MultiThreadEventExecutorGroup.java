package io.gwynt.core.concurrent;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MultiThreadEventExecutorGroup extends AbstractEventExecutorGroup {

    private final AtomicInteger childIndex = new AtomicInteger();
    private final EventExecutor[] children;
    private final ExecutorChooser chooser;
    private final Set<EventExecutor> readonlyChildren;
    private final FutureGroup shutdownFutureGroup;

    protected MultiThreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        this(nThreads, threadFactory == null ? null : new ThreadPerTaskExecutor(threadFactory), args);
    }

    protected MultiThreadEventExecutorGroup(int nThreads, Executor executor, Object... args) {
        if (nThreads < 1) {
            throw new IllegalArgumentException("nThreads > 1");
        }

        if (executor == null) {
            executor = new ThreadPerTaskExecutor();
        }

        children = new EventExecutor[nThreads];
        if (isPowerOfTwo(nThreads)) {
            chooser = new PowerOfTwoExecutorChooser();
        } else {
            chooser = new GenericExecutorChooser();
        }

        for (int i = 0; i < children.length; i++) {
            children[i] = newEventExecutor(executor, args);
        }

        Set<EventExecutor> readonlyChildren = new LinkedHashSet<>(children.length);
        Collections.addAll(readonlyChildren, children);
        this.readonlyChildren = Collections.unmodifiableSet(readonlyChildren);

        Set<Future> shutdownFutures = new HashSet<>();
        for (EventExecutor e : readonlyChildren) {
            shutdownFutures.add(e.shutdownFuture());
        }
        shutdownFutureGroup = new DefaultFutureGroup(shutdownFutures);
    }

    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }

    protected abstract EventExecutor newEventExecutor(Executor executor, Object... args);

    @Override
    public void shutdown() {
        for (EventExecutor c : children) {
            c.shutdown();
        }
    }

    @Override
    public FutureGroup<?> shutdownGracefully() {
        for (EventExecutor c : children) {
            c.shutdownGracefully();
        }
        return shutdownFutureGroup;
    }

    @Override
    public FutureGroup<?> shutdownFuture() {
        return shutdownFutureGroup;
    }

    @Override
    public EventExecutor next() {
        return chooser.next();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends EventExecutor> Set<E> children() {
        return (Set<E>) readonlyChildren;
    }

    @Override
    public boolean isShutdown() {
        for (EventExecutor c : children) {
            if (!c.isShutdown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    private interface ExecutorChooser {
        EventExecutor next();
    }

    private final class PowerOfTwoExecutorChooser implements ExecutorChooser {
        @Override
        public EventExecutor next() {
            return children[childIndex.getAndIncrement() & children.length - 1];
        }
    }

    private final class GenericExecutorChooser implements ExecutorChooser {
        @Override
        public EventExecutor next() {
            return children[childIndex.getAndIncrement() % children.length];
        }
    }
}
