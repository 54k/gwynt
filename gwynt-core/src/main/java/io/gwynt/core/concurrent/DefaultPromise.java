package io.gwynt.core.concurrent;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unchecked")
public class DefaultPromise<V> extends AbstractFuture<V> implements Promise<V> {

    private final static CancelledResult CANCELLED_RESULT = new CancelledResult(new CancellationException());
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicBoolean inNotify = new AtomicBoolean();
    private final Queue<FutureListener> listeners = new ConcurrentLinkedQueue<>();
    private volatile boolean uncancellable;
    private volatile short waiters;
    private EventExecutor eventExecutor;

    private Object result;
    private Throwable cause;

    private volatile Promise<V> chainedPromise;

    static {
        CANCELLED_RESULT.cause.setStackTrace(new StackTraceElement[0]);
    }

    public DefaultPromise() {
        this(null);
    }

    public DefaultPromise(EventExecutor eventExecutor) {
        this.eventExecutor = eventExecutor != null ? eventExecutor : GlobalEventExecutor.INSTANCE;
    }

    private static <V> void successChainedPromise(V result, Promise<V> chainedPromise) {
        chainedPromise.trySuccess(result);
    }

    private static void failChainedPromise(Throwable error, Promise<?> chainedPromise) {
        chainedPromise.tryFailure(error);
    }

    private static void cancelChainedPromise(Promise<?> chainedPromise) {
        chainedPromise.cancel();
    }

    @Override
    public boolean setUncancellable() {
        return !isDone() && (uncancellable = true);
    }

    @Override
    public boolean isUncancellable() {
        return uncancellable;
    }

    @Override
    public boolean isDone() {
        return done.get();
    }

    @Override
    public boolean isSuccess() {
        return isDone() && cause == null;
    }

    @Override
    public Future<V> await() throws InterruptedException {
        if (!isDone()) {
            synchronized (this) {
                while (!isDone()) {
                    checkDeadlock();
                    incWaiters();
                    try {
                        wait();
                    } finally {
                        decWaiters();
                    }
                }
            }
        }
        return this;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await(unit.toMillis(timeout));
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis");
        }

        if (!isDone()) {
            synchronized (this) {
                checkDeadlock();
                for (; ; ) {
                    incWaiters();
                    try {
                        wait(timeoutMillis);
                        if (!isDone()) {
                            return false;
                        }
                    } finally {
                        decWaiters();
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Promise<V> chainPromise(Promise<V> promise) {
        if (promise == null) {
            throw new IllegalArgumentException("promise");
        }
        chainPromise0(promise);
        notifyIfNeeded();
        return this;
    }

    @Override
    public Promise<V> chainPromises(Promise<V>... promises) {
        if (promises == null) {
            throw new IllegalArgumentException("promises");
        }
        for (Promise<V> p : promises) {
            chainPromise(p);
        }
        return this;
    }

    private void chainPromise0(Promise<V> promise) {
        if (chainedPromise == null) {
            chainedPromise = promise;
        } else {
            chainedPromise.chainPromise(promise);
        }
    }

    @Override
    public Future<V> addListener(FutureListener<? extends Future<? super V>> futureListener) {
        if (futureListener == null) {
            throw new IllegalArgumentException("futureListener");
        }

        listeners.add(futureListener);
        notifyIfNeeded();
        return this;
    }

    @Override
    public Future<V> addListeners(FutureListener<? extends Future<? super V>>... futureListeners) {
        if (futureListeners == null) {
            throw new IllegalArgumentException("futureListeners");
        }

        for (FutureListener<? extends Future<? super V>> l : futureListeners) {
            addListener(l);
        }
        return this;
    }

    @Override
    public Future<V> removeListener(FutureListener<? extends Future<? super V>> futureListener) {
        if (futureListener == null) {
            throw new IllegalArgumentException("futureListener");
        }

        listeners.remove(futureListener);
        return this;
    }

    @Override
    public Future<V> removeListeners(FutureListener<? extends Future<? super V>>... futureListeners) {
        if (futureListeners == null) {
            throw new IllegalArgumentException("futureListeners");
        }

        listeners.removeAll(Arrays.asList(futureListeners));
        return this;
    }

    protected void notifyListeners() {
        FutureListener futureListener;
        while ((futureListener = listeners.poll()) != null) {
            notifyListener(futureListener);
        }
    }

    protected void notifyListener(final FutureListener futureListener) {
        if (executor().inExecutorThread()) {
            futureListener.onComplete(DefaultPromise.this);
        } else {
            invokeLater(new Runnable() {
                @Override
                public void run() {
                    futureListener.onComplete(DefaultPromise.this);
                }
            });
        }
    }

    private void notifyIfNeeded() {
        if (isDone()) {
            notifyAllListeners();
        }
    }

    private void notifyAllListeners() {
        if (inNotify.getAndSet(true)) {
            invokeLater(new Runnable() {
                @Override
                public void run() {
                    notifyAllListeners();
                }
            });
        } else {
            notifyListeners();
            notifyChainedPromise();
            inNotify.set(false);
        }
    }

    private void notifyChainedPromise() {
        if (chainedPromise == null) {
            return;
        }

        final Object res = result != CANCELLED_RESULT ? result : null;
        final Promise<V> promise = chainedPromise;

        if (isSuccess()) {
            if (executor().inExecutorThread()) {
                successChainedPromise((V) res, promise);
            } else {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        successChainedPromise((V) res, promise);
                    }
                });
            }
        } else if (isCancelled()) {
            if (executor().inExecutorThread()) {
                cancelChainedPromise(promise);
            } else {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        cancelChainedPromise(promise);
                    }
                });
            }
        } else {
            if (executor().inExecutorThread()) {
                failChainedPromise(getCause(), promise);
            } else {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        failChainedPromise(getCause(), promise);
                    }
                });
            }
        }

        chainedPromise = null;
    }

    protected EventExecutor executor() {
        return eventExecutor;
    }

    private void invokeLater(Runnable task) {
        executor().execute(task);
    }

    private void checkDeadlock() {
        EventExecutor eventExecutor = executor();
        if (eventExecutor != null && eventExecutor.inExecutorThread()) {
            throw new BlockingOperationException();
        }
    }

    @Override
    public V getNow() {
        if (result == CANCELLED_RESULT) {
            return null;
        }

        return (V) result;
    }

    @Override
    public boolean trySuccess(V result) {
        if (isDone()) {
            return false;
        }
        setSuccess(result);
        return true;
    }

    @Override
    public Promise<V> setFailure(Throwable cause) {
        if (done.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        }
        this.cause = cause;
        synchronized (this) {
            if (hasWaiters()) {
                notifyAll();
            }
        }
        notifyAllListeners();
        return this;
    }

    @Override
    public boolean tryFailure(Throwable error) {
        if (isDone()) {
            return false;
        }
        setFailure(error);
        return true;
    }

    @Override
    public Promise<V> setSuccess(V result) {
        if (done.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        }
        this.result = result;
        synchronized (this) {
            if (hasWaiters()) {
                notifyAll();
            }
        }
        notifyAllListeners();
        return this;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    protected void incWaiters() {
        if (waiters == Short.MAX_VALUE) {
            throw new IllegalStateException("Too many waiters");
        }
        waiters++;
    }

    protected void decWaiters() {
        waiters--;
    }

    protected boolean hasWaiters() {
        return waiters > 0;
    }

    @Override
    public boolean isCancelled() {
        return isDone() && result == CANCELLED_RESULT;
    }

    @Override
    public boolean cancel() {
        return cancel(false);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (uncancellable) {
            return false;
        }

        if (done.getAndSet(true)) {
            return false;
        }

        result = CANCELLED_RESULT;
        synchronized (this) {
            if (hasWaiters()) {
                notifyAll();
            }
        }
        notifyAllListeners();
        return true;
    }

    private static class CancelledResult {

        private CancellationException cause;

        private CancelledResult(CancellationException cause) {
            this.cause = cause;
        }
    }
}
