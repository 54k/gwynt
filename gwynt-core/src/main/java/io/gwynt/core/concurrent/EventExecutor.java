package io.gwynt.core.concurrent;

public interface EventExecutor extends EventExecutorGroup {

    boolean inExecutorThread();

    boolean inExecutorThread(Thread thread);

    EventExecutorGroup parent();

    <V> Promise<V> newPromise();

    boolean isShuttingDown();
}
