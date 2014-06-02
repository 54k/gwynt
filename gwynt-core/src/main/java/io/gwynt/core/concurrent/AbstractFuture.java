package io.gwynt.core.concurrent;

import io.gwynt.core.exception.FutureExecutionException;
import io.gwynt.core.exception.FutureTimeoutException;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractFuture<V> implements Future<V> {

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
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
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
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
    public V get(long timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException {
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
    public Future<V> sync() throws InterruptedException {
        await();
        Throwable cause = getCause();
        if (cause == null) {
            return this;
        }
        throw new FutureExecutionException(cause);
    }

    @Override
    public Future<V> sync(long timeout, TimeUnit unit) throws InterruptedException {
        return sync(unit.toMillis(timeout));
    }

    @Override
    public Future<V> sync(long timeoutMillis) throws InterruptedException {
        if (await(timeoutMillis)) {
            Throwable cause = getCause();
            if (cause == null) {
                return this;
            }
            throw new FutureExecutionException(cause);
        }
        throw new FutureTimeoutException();
    }
}
