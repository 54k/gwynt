package io.gwynt.core;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultChannelPromise implements ChannelPromise {

    private static final TimeoutException timeoutException = new TimeoutException();

    private final Channel channel;
    private final CountDownLatch lock = new CountDownLatch(1);

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean done = new AtomicBoolean();

    private Throwable error;
    private Queue<ChannelFutureListener> listeners = new ConcurrentLinkedQueue<>();
    private Queue<ChannelPromise> promises = new ConcurrentLinkedQueue<>();


    public DefaultChannelPromise(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public void addListener(ChannelFutureListener<? extends Channel> callback) {
        listeners.add(callback);
        if (isDone()) {
            notifyListeners();
        }
    }

    @Override
    public void addListener(ChannelPromise channelPromise) {
        promises.add(channelPromise);
        if (isDone()) {
            notifyPromises();
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
            final ChannelFutureListener channelFutureListener = listeners.poll();
            if (channel.scheduler().inSchedulerThread()) {
                channelFutureListener.onComplete(channel);
            } else {
                channel.scheduler().schedule(new Runnable() {
                    @Override
                    public void run() {
                        channelFutureListener.onComplete(channel);
                    }
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void notifyListenersOnError() {
        while (listeners.peek() != null) {
            final ChannelFutureListener channelFutureListener = listeners.poll();
            if (channel.scheduler().inSchedulerThread()) {
                channelFutureListener.onError(channel, error);
            } else {
                channel.scheduler().schedule(new Runnable() {
                    @Override
                    public void run() {
                        channelFutureListener.onError(channel, error);
                    }
                });
            }
        }
    }

    private void notifyPromises() {
        if (error == null) {
            notifyPromisesOnComplete();
        } else {
            notifyPromiseOnError();
        }
    }

    private void notifyPromisesOnComplete() {
        while (promises.peek() != null) {
            promises.poll().success();
        }
    }

    private void notifyPromiseOnError() {
        while (promises.peek() != null) {
            promises.poll().fail(error);
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
    public Channel await() throws Throwable {
        try {
            lock.await();
            if (error != null) {
                throw error;
            }
            return channel;
        } catch (InterruptedException ignore) {
        }
        return null;
    }

    @Override
    public Channel await(long timeout, TimeUnit unit) throws Throwable {
        if (lock.await(timeout, unit)) {
            return get();
        }
        return null;
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
    public void fail(Throwable error) {
        if (!done.getAndSet(true)) {
            this.error = error;
            lock.countDown();
            notifyListeners();
            notifyPromises();
        }
    }

    @Override
    public void success() {
        fail(null);
    }
}