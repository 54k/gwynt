package io.gwynt.core.concurrent;

public interface Promise<T> extends Future<T> {

    T complete(T result);

    T fail(Throwable error);

    Promise<T> chainPromise(Promise<T> promise, Promise<T>... promises);
}
