package io.gwynt.core.concurrent;

public interface Promise<T> extends Future<T> {

    boolean trySuccess(T result);

    Promise<T> setSuccess(T result);

    boolean tryFailure(Throwable error);

    Promise<T> setFailure(Throwable error);

    Promise<T> chainPromise(Promise<T> promise);

    Promise<T> chainPromise(Promise<T>... promises);
}
