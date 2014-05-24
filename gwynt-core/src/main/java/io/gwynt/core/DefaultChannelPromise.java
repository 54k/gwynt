package io.gwynt.core;

import io.gwynt.core.exception.ChannelFutureFailedException;
import io.gwynt.core.exception.ChannelFutureInterruptedException;
import io.gwynt.core.exception.ChannelFutureTimeoutException;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultChannelPromise implements ChannelPromise {

    private static final ChannelFutureTimeoutException TIMEOUT_EXCEPTION = new ChannelFutureTimeoutException();

    private final Channel channel;
    private final CountDownLatch lock = new CountDownLatch(1);
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicBoolean notifyingListeners = new AtomicBoolean();
    private final AtomicBoolean notifyingPromises = new AtomicBoolean();

    private Queue<ChannelFutureListener> listeners = new ConcurrentLinkedQueue<>();
    private Queue<ChannelPromise> promises = new ConcurrentLinkedQueue<>();

    private Throwable error;

    public DefaultChannelPromise(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public ChannelFuture addListener(ChannelFutureListener callback, ChannelFutureListener... callbacks) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        listeners.add(callback);
        Collections.addAll(listeners, callbacks);
        if (isDone()) {
            notifyListeners();
        }
        return this;
    }

    @Override
    public ChannelPromise chainPromise(ChannelPromise channelPromise, ChannelPromise... channelPromises) {
        if (channelPromise == null) {
            throw new IllegalArgumentException("channelPromise");
        }

        promises.add(channelPromise);
        Collections.addAll(promises, channelPromises);
        if (isDone()) {
            notifyPromises();
        }
        return this;
    }

    private void notifyListeners() {
        if (notifyingListeners.getAndSet(true)) {
            channel().eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    notifyListeners();
                }
            });
            return;
        }
        while (listeners.peek() != null) {
            final ChannelFutureListener channelFutureListener = listeners.poll();
            if (channel.eventLoop().inExecutorThread()) {
                channelFutureListener.onComplete(this);
            } else {
                channel.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        channelFutureListener.onComplete(DefaultChannelPromise.this);
                    }
                });
            }
        }
        notifyingListeners.set(false);
    }

    private void notifyPromises() {
        if (notifyingPromises.getAndSet(true)) {
            channel().eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    notifyPromises();
                }
            });
            return;
        }
        while (promises.peek() != null) {
            final ChannelPromise promise = promises.poll();
            if (channel.eventLoop().inExecutorThread()) {
                promise.complete(error);
            } else {
                channel.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        promise.complete(error);
                    }
                });
            }
        }
        notifyingPromises.set(false);
    }

    @Override
    public boolean isDone() {
        return done.get();
    }

    @Override
    public boolean isFailed() {
        return error != null;
    }

    @Override
    public Throwable getError() {
        return error;
    }

    @Override
    public ChannelFuture await() {
        try {
            lock.await();
        } catch (InterruptedException e) {
            throw new ChannelFutureInterruptedException();
        }

        if (error != null) {
            throw new ChannelFutureFailedException(error);
        }
        return this;
    }

    @Override
    public ChannelFuture await(long timeout, TimeUnit unit) {
        try {
            if (lock.await(timeout, unit)) {
                return await();
            }
        } catch (InterruptedException e) {
            throw new ChannelFutureInterruptedException();
        }
        throw TIMEOUT_EXCEPTION;
    }

    @Override
    public ChannelPromise complete(Throwable error) {
        boolean wasDone = done.getAndSet(true);
        if (!wasDone) {
            this.error = error;
            lock.countDown();
            notifyListeners();
            notifyPromises();
        }
        return this;
    }

    @Override
    public ChannelPromise complete() {
        return complete(null);
    }
}
