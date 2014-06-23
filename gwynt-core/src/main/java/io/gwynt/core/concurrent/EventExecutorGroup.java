package io.gwynt.core.concurrent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface EventExecutorGroup extends ScheduledExecutorService {

    EventExecutor next();

    <E extends EventExecutor> Set<E> children();

    @Deprecated
    @Override
    List<Runnable> shutdownNow();

    @Deprecated
    @Override
    void shutdown();

    FutureGroup<?> shutdownGracefully();

    FutureGroup<?> shutdownFutureGroup();

    @Override
    <T> Future<T> submit(Callable<T> task);

    @Override
    <T> Future<T> submit(Runnable task, T result);

    @Override
    Future<?> submit(Runnable task);

    @Override
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    @Override
    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    @Override
    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    @Override
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
