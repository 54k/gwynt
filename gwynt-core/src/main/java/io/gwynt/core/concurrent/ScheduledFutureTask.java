package io.gwynt.core.concurrent;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ScheduledFutureTask<V> extends PromiseTask<V> implements ScheduledFuture<V> {

    private static final long START_TIME = System.currentTimeMillis();

    private Queue<ScheduledFutureTask<?>> delayedTaskQueue;
    private long deadlineMillis;
    /* 0 - no repeat, >0 - repeat at fixed rate, <0 - repeat with fixed delay */
    private long period;

    ScheduledFutureTask(EventExecutor eventExecutor, Callable<V> task, long deadlineMillis, Queue<ScheduledFutureTask<?>> delayedTaskQueue) {
        super(eventExecutor, task);
        period = 0;
        this.deadlineMillis = deadlineMillis;
        this.delayedTaskQueue = delayedTaskQueue;
    }

    ScheduledFutureTask(EventExecutor eventExecutor, Callable<V> task, long deadlineMillis, long period, Queue<ScheduledFutureTask<?>> delayedTaskQueue) {
        super(eventExecutor, task);
        this.deadlineMillis = deadlineMillis;
        this.period = period;
        this.delayedTaskQueue = delayedTaskQueue;
    }

    static long timeMillis() {
        return System.currentTimeMillis() - START_TIME;
    }

    static long deadlineMillis(long delay) {
        return timeMillis() + delay;
    }

    long deadlineMillis() {
        return deadlineMillis;
    }

    private long delayMillis() {
        return Math.max(0, deadlineMillis - timeMillis());
    }

    public long delayMillis(long currentTimeMillis) {
        return Math.max(0, deadlineMillis() - (currentTimeMillis - START_TIME));
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delayMillis(), TimeUnit.MILLISECONDS);
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
                            deadlineMillis += p;
                        } else {
                            deadlineMillis = timeMillis() - p;
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
        long d = delayMillis() - that.delayMillis();
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
