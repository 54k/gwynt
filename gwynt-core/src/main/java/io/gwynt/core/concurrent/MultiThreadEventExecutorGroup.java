package io.gwynt.core.concurrent;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MultiThreadEventExecutorGroup extends AbstractEventExecutorGroup {

    private final AtomicInteger childIndex = new AtomicInteger();
    private final EventExecutor[] children;
    private final Chooser chooser;

    protected MultiThreadEventExecutorGroup() {
        this(Runtime.getRuntime().availableProcessors() * 2);
    }

    protected MultiThreadEventExecutorGroup(int nThreads) {
        this(nThreads, new ThreadPerTaskExecutor());
    }

    protected MultiThreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, new ThreadPerTaskExecutor(threadFactory));
    }

    protected MultiThreadEventExecutorGroup(int nThreads, Executor executor) {
        children = new EventExecutor[nThreads];

        if (isPowerOfTwo(nThreads)) {
            chooser = new PowerOfTwoChooser();
        } else {
            chooser = new GenericChooser();
        }

        for (int i = 0; i < children.length; i++) {
            children[i] = newEventExecutor(executor);
        }
    }

    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }

    protected abstract EventExecutor newEventExecutor(Executor executor);

    @Override
    public void shutdown() {
        for (EventExecutor c : children) {
            c.shutdown();
        }
    }

    @Override
    public EventExecutor next() {
        return chooser.next();
    }

    @Override
    public <E extends EventExecutor> Set<E> children() {
        return null;
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

    private interface Chooser {
        EventExecutor next();
    }

    private final class PowerOfTwoChooser implements Chooser {
        @Override
        public EventExecutor next() {
            return children[childIndex.getAndIncrement() & children.length - 1];
        }
    }

    private final class GenericChooser implements Chooser {
        @Override
        public EventExecutor next() {
            return children[childIndex.getAndIncrement() % children.length];
        }
    }
}
