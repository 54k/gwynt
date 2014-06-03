package io.gwynt.core.concurrent;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ScheduledFutureTask<V> extends PromiseTask<V> implements ScheduledFuture<V> {

    private static final long START_NANOS = System.nanoTime();

    /* 0 - no repeat, >0 - repeat at fixed rate, <0 - repeat with fixed delay */
    private long period;
    private long deadlineNanos;
    private Queue<ScheduledFutureTask<?>> delayedTaskQueue;

    ScheduledFutureTask(EventExecutor eventExecutor, Callable<V> task, long deadlineNanos, Queue<ScheduledFutureTask<?>> delayedTaskQueue) {
        super(eventExecutor, task);
        period = 0;
        this.deadlineNanos = deadlineNanos;
        this.delayedTaskQueue = delayedTaskQueue;
    }

    ScheduledFutureTask(EventExecutor eventExecutor, Callable<V> task, long deadlineNanos, long period, Queue<ScheduledFutureTask<?>> delayedTaskQueue) {
        super(eventExecutor, task);
        this.deadlineNanos = deadlineNanos;
        this.period = period;
        this.delayedTaskQueue = delayedTaskQueue;
    }

    static long nanoTime() {
        return System.nanoTime() - START_NANOS;
    }

    static long deadlineNanos(long delayNanos) {
        return nanoTime() + delayNanos;
    }

    long deadlineNanos() {
        return deadlineNanos;
    }

    private long delayNanos() {
        return Math.max(0, deadlineNanos - nanoTime());
    }

    public long delayNanos(long currentTimeNanos) {
        return Math.max(0, deadlineNanos() - (currentTimeNanos - START_NANOS));
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delayNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public void run() {
        assert executor().inExecutorThread();
        try {
            if (period == 0) {
                if (setUncancellableInternal()) {
                    V result = task.call();
                    setSuccessInternal(result);
                }
            } else {
                if (!isCancelled()) {
                    task.call();
                    if (!executor().isShutdown()) {
                        long p = period;
                        if (p > 0) {
                            deadlineNanos += p;
                        } else {
                            deadlineNanos = nanoTime() - p;
                        }

                        if (!isCancelled()) {
                            delayedTaskQueue.add(this);
                        }
                    }
                }
            }
        } catch (Throwable cause) {
            setFailureInternal(cause);
        }
    }

    @Override
    public int compareTo(Delayed o) {
        if (o == null) {
            throw new IllegalArgumentException("o");
        }

        if (this == o) {
            return 0;
        }

        ScheduledFutureTask<?> that = (ScheduledFutureTask<?>) o;
        long d = delayNanos() - that.delayNanos();
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
