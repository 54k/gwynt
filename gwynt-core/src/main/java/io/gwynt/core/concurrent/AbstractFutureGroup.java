package io.gwynt.core.concurrent;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public abstract class AbstractFutureGroup<V, T extends Future> extends DefaultPromise<V> implements Future<V> {

    private int successCount;
    private int failureCount;

    protected AbstractFutureGroup() {
    }

    protected AbstractFutureGroup(EventExecutor eventExecutor) {
        super(eventExecutor);
    }

    protected abstract void notify0();

    protected void setSuccess0() {
        super.setSuccess(null);
    }

    protected void setFailure0(Throwable cause) {
        super.setFailure(cause);
    }

    protected void count(Future future) {
        synchronized (AbstractFutureGroup.this) {
            if (future.isFailed()) {
                failureCount++;
            } else {
                successCount++;
            }
        }

        notify0();
    }

    protected int successCount() {
        return successCount;
    }

    protected int failureCount() {
        return failureCount;
    }

    @Override
    public Promise<V> setFailure(Throwable cause) {
        throw new IllegalStateException();
    }

    @Override
    public boolean tryFailure(Throwable error) {
        return false;
    }

    @Override
    public Promise<V> setSuccess(V result) {
        throw new IllegalStateException();
    }

    @Override
    public boolean trySuccess(V result) {
        return false;
    }

    @Override
    public T addListener(FutureListener<? extends Future<? super V>> futureListener) {
        return (T) super.addListener(futureListener);
    }

    @Override
    public T addListeners(FutureListener<? extends Future<? super V>>... futureListeners) {
        return (T) super.addListeners(futureListeners);
    }

    @Override
    public T removeListener(FutureListener<? extends Future<? super V>> futureListener) {
        return (T) super.removeListener(futureListener);
    }

    @Override
    public T removeListeners(FutureListener<? extends Future<? super V>>... futureListeners) {
        return (T) super.removeListeners(futureListeners);
    }

    @Override
    public T await() throws InterruptedException {
        return (T) this;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return false;
    }

    @Override
    public T sync() throws InterruptedException {
        super.sync();
        return (T) this;
    }

    @Override
    public T sync(long timeout, TimeUnit unit) throws InterruptedException {
        super.sync(timeout, unit);
        return (T) this;
    }

    @Override
    public T sync(long timeoutMillis) throws InterruptedException {
        return (T) this;
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    protected static final class DefaultEntry<K, V> implements Map.Entry<K, V> {

        private final K key;
        private final V value;

        public DefaultEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("read-only");
        }
    }
}
