package io.gwynt.core.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;

public class PromiseTask<V> extends DefaultPromise<V> implements RunnableFuture<V> {

    protected final Callable<V> task;

    PromiseTask(EventExecutor eventExecutor, V result, Runnable task) {
        this(eventExecutor, toCallable(result, task));
    }

    PromiseTask(V result, Runnable task) {
        this(toCallable(result, task));
    }

    PromiseTask(Callable<V> task) {
        this(null, task);
    }

    PromiseTask(EventExecutor eventExecutor, Callable<V> task) {
        super(eventExecutor);
        this.task = task;
    }

    static <V> Callable<V> toCallable(V result, Runnable task) {
        return new RunnableAdapter<>(result, task);
    }

    @Override
    public void run() {
        try {
            if (!isCancelled()) {
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

    private PromiseTask<V> setFailureInternal(Throwable cause) {
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

    public PromiseTask<V> setSuccessInternal(V result) {
        super.setSuccess(result);
        return this;
    }

    private static class RunnableAdapter<V> implements Callable<V> {

        private V result;
        private Runnable task;

        private RunnableAdapter(V result, Runnable task) {
            this.result = result;
            this.task = task;
        }

        @Override
        public V call() throws Exception {
            task.run();
            return result;
        }
    }
}
