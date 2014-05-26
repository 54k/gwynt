package io.gwynt.core.concurrent;

import java.util.concurrent.TimeUnit;

public interface Future<T> extends java.util.concurrent.Future<T> {

    boolean isFailed();

    T getNow();

    Throwable getCause();

    Future<T> await() throws InterruptedException;

    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    boolean await(long timeoutMillis) throws InterruptedException;

    Future<T> addListener(FutureListener futureListener, FutureListener... futureListeners);
}
