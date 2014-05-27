package io.gwynt.core.concurrent;

import io.gwynt.core.exception.FutureExecutionException;
import io.gwynt.core.exception.FutureTimeoutException;

import java.util.concurrent.CancellationException;
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
            if (isCancelled()) {
                throw new CancellationException();
            }
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

    @Override
    public T get(long timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException {
        if (await(timeoutMillis)) {
            Throwable cause = getCause();
            if (cause == null) {
                if (isCancelled()) {
                    throw new CancellationException();
                }
                return getNow();
            }
            throw new ExecutionException(cause);
        }
        throw new TimeoutException();
    }

    @Override
    public T sync() throws InterruptedException {
        await();
        Throwable cause = getCause();
        if (cause == null) {
            return getNow();
        }
        throw new FutureExecutionException(cause);
    }

    @Override
    public T sync(long timeout, TimeUnit unit) throws InterruptedException {
        return sync(unit.toMillis(timeout));
    }

    @Override
    public T sync(long timeoutMillis) throws InterruptedException {
        if (await(timeoutMillis)) {
            Throwable cause = getCause();
            if (cause == null) {
                return getNow();
            }
            throw new FutureExecutionException(cause);
        }
        throw new FutureTimeoutException();
    }
}
