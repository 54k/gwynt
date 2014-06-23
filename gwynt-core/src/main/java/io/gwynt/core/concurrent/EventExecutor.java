package io.gwynt.core.concurrent;

import java.util.concurrent.TimeUnit;

public interface EventExecutor extends EventExecutorGroup {

    boolean inExecutorThread();

    boolean inExecutorThread(Thread thread);

    EventExecutorGroup parent();

    <V> Promise<V> newPromise();

    long lastExecutionTimeNanos();

    long lastExecutionTime(TimeUnit timeUnit);

    Future<?> shutdownGracefully();

    Future<?> shutdownFuture();
}
