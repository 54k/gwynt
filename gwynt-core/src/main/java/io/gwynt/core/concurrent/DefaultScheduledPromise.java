package io.gwynt.core.concurrent;

import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultScheduledPromise<T> extends DefaultPromise<T> implements ScheduledFuture<T> {

    private static final long START_TIME = System.currentTimeMillis();

    private long deadlineMillis;

    public DefaultScheduledPromise(long deadlineMillis) {
        this(null, deadlineMillis);
    }

    public DefaultScheduledPromise(EventExecutor eventExecutor, long deadlineMillis) {
        super(eventExecutor);
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

        DefaultScheduledPromise<?> that = (DefaultScheduledPromise<?>) o;
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
