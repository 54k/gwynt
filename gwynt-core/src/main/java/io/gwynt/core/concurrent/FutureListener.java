package io.gwynt.core.concurrent;

public interface FutureListener<T extends Future> {

    void onComplete(T result);
}
