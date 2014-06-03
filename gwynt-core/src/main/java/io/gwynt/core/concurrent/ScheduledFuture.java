package io.gwynt.core.concurrent;

import java.util.concurrent.TimeUnit;

public interface ScheduledFuture<V> extends Future<V>, java.util.concurrent.ScheduledFuture<V> {

    public long getDelayNanos();

    public long getDelayNanos(long timeNanos);

    public long getDelay(long time, TimeUnit unit);
}
