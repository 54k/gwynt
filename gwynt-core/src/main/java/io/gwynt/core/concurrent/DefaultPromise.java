package io.gwynt.core.concurrent;

import io.gwynt.core.EventExecutor;
import io.gwynt.core.exception.BlockingOperationException;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultPromise<T> extends AbstractFuture<T> implements Promise<T> {

    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicBoolean inNotify = new AtomicBoolean();
    private final Queue<FutureListener> listeners = new ConcurrentLinkedQueue<>();
    private volatile short waiters;
    private EventExecutor eventExecutor;
    private T result;
    private Throwable cause;
    private volatile Promise<T> chainedPromise;

    public DefaultPromise() {
        this(null);
    }

    public DefaultPromise(EventExecutor eventExecutor) {
        this.eventExecutor = eventExecutor;
    }

    @Override
    public boolean isDone() {
        return done.get();
    }

    @Override
    public boolean isFailed() {
        return isDone() && cause != null;
    }

    @Override
    public Promise<T> await() throws InterruptedException {
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

    @SafeVarargs
    @Override
    public final Promise<T> chainPromise(Promise<T> promise, Promise<T>... promises) {
        if (promise == null) {
            throw new IllegalArgumentException("channelPromise");
        }
        chainPromise(promise);
        for (Promise<T> p : promises) {
            chainPromise(p);
        }
        notifyIfNeeded();
        return this;
    }

    @SuppressWarnings("unchecked")
    private void chainPromise(Promise<T> promise) {
        if (chainedPromise == null) {
            chainedPromise = promise;
        } else {
            chainedPromise.chainPromise(promise);
        }
    }

    @Override
    public Future<T> addListener(FutureListener futureListener, FutureListener... futureListeners) {
        if (futureListener == null) {
            throw new IllegalArgumentException("futureListener");
        }
        listeners.add(futureListener);
        Collections.addAll(listeners, futureListeners);
        notifyIfNeeded();
        return this;
    }

    protected void notifyListeners() {
        FutureListener futureListener;
        while ((futureListener = listeners.poll()) != null) {
            notifyListener(futureListener);
        }
    }

    @SuppressWarnings("unchecked")
    protected void notifyListener(final FutureListener futureListener) {
        if (executor().inExecutorThread()) {
            futureListener.onComplete(DefaultPromise.this);
        } else {
            execute(new Runnable() {
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
            execute(new Runnable() {
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

        if (isFailed()) {
            if (executor().inExecutorThread()) {
                chainedPromise.fail(getCause());
            } else {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        chainedPromise.fail(getCause());
                    }
                });
            }
        } else {
            if (executor().inExecutorThread()) {
                chainedPromise.complete(result);
            } else {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        chainedPromise.complete(result);
                    }
                });
            }
        }
    }

    protected EventExecutor executor() {
        return eventExecutor;
    }

    private void execute(Runnable task) {
        executor().execute(task);
    }

    private void checkDeadlock() {
        EventExecutor eventExecutor = executor();
        if (eventExecutor != null && eventExecutor.inExecutorThread()) {
            throw new BlockingOperationException();
        }
    }

    @Override
    public T getNow() {
        return result;
    }

    @Override
    public T fail(Throwable cause) {
        if (done.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        }
        this.cause = cause;
        synchronized (this) {
            if (hasWaiters()) {
                notifyAll();
            }
        }
        return result;
    }

    @Override
    public T complete(T result) {
        if (done.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        }
        this.result = result;
        synchronized (this) {
            if (hasWaiters()) {
                notifyAll();
            }
        }
        return result;
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
}
