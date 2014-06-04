package io.gwynt.core.concurrent;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MultiThreadEventExecutor extends AbstractEventExecutorGroup {

    private final AtomicInteger childIndex = new AtomicInteger();
    private final EventExecutor[] children;
    private final Chooser chooser;

    protected MultiThreadEventExecutor() {
        this(Runtime.getRuntime().availableProcessors() * 2);
    }

    protected MultiThreadEventExecutor(int nThreads) {
        children = new EventExecutor[nThreads];

        if (isPowerOfTwo(nThreads)) {
            chooser = new PowerOfTwoChooser();
        } else {
            chooser = new RoundRobinChooser();
        }
    }

    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }

    protected abstract EventExecutor newEventExecutor();

    @Override
    public void shutdown() {
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
        return false;
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

    private final class RoundRobinChooser implements Chooser {
        @Override
        public EventExecutor next() {
            return children[childIndex.getAndIncrement() % children.length];
        }
    }
}
