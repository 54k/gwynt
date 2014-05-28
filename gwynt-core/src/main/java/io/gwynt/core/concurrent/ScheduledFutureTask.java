package io.gwynt.core.concurrent;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ScheduledFutureTask<V> extends PromiseTask<V> implements ScheduledFuture<V> {

    private static final long START_TIME = System.currentTimeMillis();
    private long deadline;
    private long period;
    private Queue<ScheduledFutureTask<?>> delayedTaskQueue;

    // TODO add constructors
    public ScheduledFutureTask(Callable<V> task) {
        super(task);
    }

    static long time() {
        return System.currentTimeMillis() - START_TIME;
    }

    long deadline() {
        return deadline;
    }

    private long delay() {
        return Math.max(0, deadline - time());
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delay(), TimeUnit.MILLISECONDS);
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
                            deadline += p;
                        } else {
                            deadline = time() - p;
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
        long d = delay() - that.delay();
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
