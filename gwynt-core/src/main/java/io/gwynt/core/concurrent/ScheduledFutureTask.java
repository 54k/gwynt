package io.gwynt.core.concurrent;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ScheduledFutureTask<V> extends PromiseTask<V> implements ScheduledFuture<V> {

    private static final long ORIGIN = System.nanoTime();
    private static final AtomicLong SEQUENCER = new AtomicLong(0);

    private long seq = SEQUENCER.getAndIncrement();

    /**
     * Period in nanoseconds for repeating tasks.  A positive
     * value indicates fixed-rate execution.  A negative value
     * indicates fixed-delay execution.  A value of 0 indicates a
     * non-repeating task.
     */
    private long period;
    private long triggerTime;
    private Queue<ScheduledFutureTask<?>> delayedTaskQueue;

    ScheduledFutureTask(EventExecutor eventExecutor, Callable<V> task, long delay, Queue<ScheduledFutureTask<?>> delayedTaskQueue) {
        super(eventExecutor, task);
        period = 0;
        this.triggerTime = delay;
        this.delayedTaskQueue = delayedTaskQueue;
    }

    ScheduledFutureTask(EventExecutor eventExecutor, Callable<V> task, long delay, long period, Queue<ScheduledFutureTask<?>> delayedTaskQueue) {
        super(eventExecutor, task);
        this.triggerTime = delay;
        this.period = period;
        this.delayedTaskQueue = delayedTaskQueue;
    }

    static long nanos() {
        return System.nanoTime() - ORIGIN;
    }

    static long triggerTime(long delayNanos) {
        return nanos() + delayNanos;
    }

    long triggerTime() {
        return triggerTime;
    }

    @Override
    public long getDelayNanos() {
        return Math.max(0, triggerTime - nanos());
    }

    @Override
    public long getDelayNanos(long timeNanos) {
        return Math.max(0, triggerTime() - (timeNanos - ORIGIN));
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(getDelayNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public long getDelay(long time, TimeUnit unit) {
        return unit.convert(getDelayNanos(unit.toNanos(time)), TimeUnit.NANOSECONDS);
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
                        long nanos = nanos();

                        if (p > 0) {
                            long e = nanos - triggerTime;
                            if (e % p > 1) {
                                triggerTime = nanos - p;
                            } else {
                                triggerTime += p;
                            }
                        } else {
                            triggerTime = nanos - p;
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

        if (o instanceof ScheduledFutureTask) {
            ScheduledFutureTask<?> that = (ScheduledFutureTask<?>) o;
            long diff = getDelayNanos() - that.getDelayNanos();
            if (diff < 0) {
                return -1;
            } else if (diff > 0) {
                return 1;
            } else if (seq < that.seq) {
                return -1;
            } else {
                return 1;
            }
        }

        long d = (getDelayNanos() - o.getDelay(TimeUnit.NANOSECONDS));
        return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }
}
