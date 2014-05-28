package io.gwynt.core.concurrent;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ScheduledFutureTask<V> extends PromiseTask<V> implements ScheduledFuture<V> {

    private static final long START_TIME = System.currentTimeMillis();
    private long deadline;
    private long period;

    static long currentTime() {
        return System.currentTimeMillis() - START_TIME;
    }

    long deadline() {
        return deadline;
    }

    private long delay() {
        return Math.max(0, deadline - currentTime());
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
                V result = task.call();
                setSuccessInternal(result);
            } else {
                // check if is done as it may was cancelled
                if (!isCancelled()) {
                    task.call();
                    if (!executor().isShutdown()) {
                        long p = period;
                        if (p > 0) {
                            deadline += p;
                        } else {
                            deadline = currentTime() - p;
                        }
                        if (!isCancelled()) {
                            //                            delayedTaskQueue.add(this);
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
