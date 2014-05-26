package io.gwynt.core.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractFuture<T> implements Future<T> {

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        await();
        Throwable cause = getCause();
        if (cause == null) {
            return getNow();
        }
        throw new ExecutionException(cause);
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (await(timeout, unit)) {
            Throwable cause = getCause();
            if (cause == null) {
                return getNow();
            }
            throw new ExecutionException(cause);
        }
        throw new TimeoutException();
    }
}
