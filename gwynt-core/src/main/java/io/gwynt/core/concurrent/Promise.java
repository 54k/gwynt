package io.gwynt.core.concurrent;

public interface Promise<T> extends Future<T> {

    Promise<T> setSuccess(T result);

    Promise<T> setFailure(Throwable error);

    Promise<T> chainPromise(Promise<T> promise);

    Promise<T> chainPromise(Promise<T>... promises);
}
