package io.gwynt.core.concurrent;

public interface FutureListener<V extends Future<?>> {

    void onComplete(V future);
}
