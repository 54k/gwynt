package io.gwynt.core.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;

public class PromiseTask<V> extends DefaultPromise<V> implements RunnableFuture<V> {

    protected final Callable<V> task;

    public PromiseTask(Callable<V> task) {
        this(null, task);
    }

    public PromiseTask(EventExecutor eventExecutor, Callable<V> task) {
        super(eventExecutor);
        this.task = task;
    }

    @Override
    public void run() {
        try {
            task.call();
        } catch (Exception ignore) {
        }
    }
}
