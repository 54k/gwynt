package io.gwynt.core;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultChannelFuture implements ChannelFuture {

    private static final TimeoutException timeoutException = new TimeoutException();

    private final Channel channel;
    private final CountDownLatch lock = new CountDownLatch(1);

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean done = new AtomicBoolean();

    private Throwable error;
    private Queue<ChannelListener> listeners = new ConcurrentLinkedQueue<>();

    public DefaultChannelFuture(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public void addListener(ChannelListener<? extends Channel> callback) {
        listeners.add(callback);
        if (isDone()) {
            notifyListeners();
        }
    }

    private void notifyListeners() {
        if (error == null) {
            notifyListenersOnComplete();
        } else {
            notifyListenersOnError();
        }
    }

    @SuppressWarnings("unchecked")
    private void notifyListenersOnComplete() {
        while (listeners.peek() != null) {
            listeners.poll().onComplete(channel);
        }
    }

    @SuppressWarnings("unchecked")
    private void notifyListenersOnError() {
        while (listeners.peek() != null) {
            listeners.poll().onError(channel, error);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (cancelled.getAndSet(true)) {
            lock.countDown();
            return true;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean isDone() {
        return done.get();
    }

    @Override
    public Channel get() throws InterruptedException, ExecutionException {
        lock.await();
        if (error != null) {
            throw new ExecutionException(error);
        }
        return channel;
    }

    @Override
    public Channel get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (lock.await(timeout, unit)) {
            return get();
        }
        throw timeoutException;
    }

    @Override
    public void complete(Throwable error) {
        if (!done.getAndSet(true)) {
            this.error = error;
            lock.countDown();
            notifyListeners();
        }
    }

    @Override
    public void complete() {
        complete(null);
    }
}
