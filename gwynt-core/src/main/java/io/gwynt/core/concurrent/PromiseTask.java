package io.gwynt.core.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;

public class PromiseTask<V> extends DefaultPromise<V> implements RunnableFuture<V> {

    protected final Callable<V> task;

    PromiseTask(EventExecutor eventExecutor, Runnable task, V result) {
        this(eventExecutor, toCallable(task, result));
    }

    PromiseTask(EventExecutor eventExecutor, Callable<V> task) {
        super(eventExecutor);
        this.task = task;
    }

    static Callable<?> toCallable(Runnable task) {
        return Executors.callable(task);
    }

    static <V> Callable<V> toCallable(Runnable task, V result) {
        return Executors.callable(task, result);
    }

    @Override
    public void run() {
        try {
            if (setUncancellableInternal()) {
                V result = task.call();
                setSuccessInternal(result);
            }
        } catch (Throwable e) {
            setFailureInternal(e);
        }
    }

    @Override
    public boolean trySuccess(V result) {
        return false;
    }

    @Override
    public Promise<V> setFailure(Throwable cause) {
        throw new IllegalStateException();
    }

    protected PromiseTask<V> setFailureInternal(Throwable cause) {
        super.setFailure(cause);
        return this;
    }

    @Override
    public boolean tryFailure(Throwable error) {
        return false;
    }

    @Override
    public Promise<V> setSuccess(V result) {
        throw new IllegalStateException();
    }

    protected PromiseTask<V> setSuccessInternal(V result) {
        super.setSuccess(result);
        return this;
    }

    @Override
    public boolean setUncancellable() {
        throw new IllegalStateException();
    }

    protected boolean setUncancellableInternal() {
        return super.setUncancellable();
    }
}
