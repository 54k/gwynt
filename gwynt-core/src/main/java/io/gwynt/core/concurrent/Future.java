package io.gwynt.core.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Future<T> extends java.util.concurrent.Future<T> {

    boolean isFailed();

    T getNow();

    Throwable getCause();

    Future<T> await() throws InterruptedException;

    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    boolean await(long timeoutMillis) throws InterruptedException;

    T get(long timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException;

    Future<T> addListener(FutureListener<? extends Future<? super T>> futureListener);

    Future<T> addListener(FutureListener<? extends Future<? super T>>... futureListeners);

    T sync() throws InterruptedException;

    T sync(long timeout, TimeUnit unit) throws InterruptedException;

    T sync(long timeoutMillis) throws InterruptedException;
}
