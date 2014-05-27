package io.gwynt.core.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledTask<V> extends PromiseTask<V> implements ScheduledFuture<V> {

    private static final long START_TIME = System.currentTimeMillis();

    private long deadlineMillis;

    public ScheduledTask(Callable<V> task, long deadlineMillis) {
        this(null, task, deadlineMillis);
    }

    public ScheduledTask(EventExecutor eventExecutor, Callable<V> task, long deadlineMillis) {
        super(eventExecutor, task);
        if (deadlineMillis < 0) {
            throw new IllegalArgumentException("deadlineMillis");
        }
        this.deadlineMillis = deadlineMillis;
    }

    private static long timeMillis() {
        return System.currentTimeMillis() - START_TIME;
    }

    private long delayMillis() {
        return Math.max(0, deadlineMillis - timeMillis());
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delayMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o == null) {
            throw new IllegalArgumentException("o");
        }

        if (this == o) {
            return 0;
        }

        ScheduledTask<?> that = (ScheduledTask<?>) o;
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
