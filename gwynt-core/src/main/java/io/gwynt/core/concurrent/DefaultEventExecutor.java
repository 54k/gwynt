package io.gwynt.core.concurrent;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public final class DefaultEventExecutor extends SingleThreadEventExecutor {

    public DefaultEventExecutor(EventExecutorGroup parent) {
        this(parent, null);
    }

    public DefaultEventExecutor(EventExecutorGroup parent, Executor executor) {
        super(parent, true, executor);
    }

    @Override
    protected Queue<Runnable> newTaskQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Override
    protected void run() {
        for (; ; ) {
            Runnable task = takeTask();
            if (task != null) {
                try {
                    task.run();
                } catch (Throwable ignore) {
                }
            }

            if (isShutdown()) {
                break;
            }
        }
    }
}
