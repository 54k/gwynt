package io.gwynt.core.concurrent;

public interface Promise<V> extends Future<V> {

    boolean setUncancellable();

    boolean trySuccess(V result);

    Promise<V> setSuccess(V result);

    boolean tryFailure(Throwable error);

    Promise<V> setFailure(Throwable error);

    Promise<V> chainPromise(Promise<V> promise);

    Promise<V> chainPromises(Promise<V>... promises);
}
