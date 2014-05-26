package io.gwynt.core;

import io.gwynt.core.exception.ChannelFutureDeadlockException;
import io.gwynt.core.exception.ChannelFutureFailedException;
import io.gwynt.core.exception.ChannelFutureInterruptedException;
import io.gwynt.core.exception.ChannelFutureTimeoutException;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultChannelPromise implements ChannelPromise {

    private static final ChannelFutureTimeoutException TIMEOUT_EXCEPTION = new ChannelFutureTimeoutException();

    private final Channel channel;
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicBoolean notifying = new AtomicBoolean();

    private final Queue<ChannelFutureListener> listeners = new ConcurrentLinkedQueue<>();

    private volatile ChannelPromise chainedPromise;
    private volatile Throwable cause;

    public DefaultChannelPromise(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public ChannelFuture addListener(ChannelFutureListener channelFutureListener, ChannelFutureListener... channelFutureListeners) {
        if (channelFutureListener == null) {
            throw new IllegalArgumentException("callback");
        }
        listeners.add(channelFutureListener);
        Collections.addAll(listeners, channelFutureListeners);
        notifyIfNeeded();
        return this;
    }

    @Override
    public ChannelPromise chainPromise(ChannelPromise channelPromise, ChannelPromise... channelPromises) {
        if (channelPromise == null) {
            throw new IllegalArgumentException("channelPromise");
        }
        chainPromise(channelPromise);
        for (ChannelPromise p : channelPromises) {
            chainPromise(p);
        }
        notifyIfNeeded();
        return this;
    }

    private void notifyIfNeeded() {
        if (isDone()) {
            notifyAllListeners();
        }
    }

    private void chainPromise(ChannelPromise channelPromise) {
        if (chainedPromise == null) {
            chainedPromise = channelPromise;
        } else {
            chainedPromise.chainPromise(channelPromise);
        }
    }

    private void notifyAllListeners() {
        if (notifying.getAndSet(true)) {
            channel.eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    notifyAllListeners();
                }
            });
        } else {
            notifyListeners();
            notifyChainedPromise();
            notifying.set(false);
        }
    }

    private void notifyListeners() {
        EventLoop eventLoop = channel.eventLoop();
        while (listeners.peek() != null) {
            final ChannelFutureListener channelFutureListener = listeners.poll();
            if (eventLoop.inExecutorThread()) {
                channelFutureListener.onComplete(this);
            } else {
                eventLoop.execute(new Runnable() {
                    @Override
                    public void run() {
                        channelFutureListener.onComplete(DefaultChannelPromise.this);
                    }
                });
            }
        }
    }

    private void notifyChainedPromise() {
        if (chainedPromise == null) {
            return;
        }
        EventLoop eventLoop = channel.eventLoop();
        if (eventLoop.inExecutorThread()) {
            chainedPromise.complete(cause);
        } else {
            eventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    chainedPromise.complete(cause);
                }
            });
        }
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
    public Throwable getCause() {
        return cause;
    }

    @Override
    public ChannelFuture await() {
        return await0(false, 0);
    }

    @Override
    public ChannelFuture await(long timeout, TimeUnit unit) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout");
        }
        return await0(true, unit.toMillis(timeout));
    }

    private ChannelFuture await0(boolean timed, long timeoutMillis) {
        if (!isDone()) {
            checkDeadlock();
            synchronized (this) {
                while (!isDone()) {
                    try {
                        if (!timed) {
                            wait();
                        } else {
                            wait(timeoutMillis);
                            if (!isDone()) {
                                throw TIMEOUT_EXCEPTION;
                            }
                        }
                    } catch (InterruptedException e) {
                        throw new ChannelFutureInterruptedException();
                    }
                }
            }
        }

        if (isFailed()) {
            throw new ChannelFutureFailedException(cause);
        }
        return this;
    }

    private void checkDeadlock() {
        EventLoop eventLoop = channel.eventLoop();
        if (eventLoop != null && eventLoop.inExecutorThread()) {
            throw new ChannelFutureDeadlockException();
        }
    }

    @Override
    public ChannelPromise complete(Throwable cause) {
        if (!done.getAndSet(true)) {
            this.cause = cause;
            notifyAllListeners();
            synchronized (this) {
                notifyAll();
            }
        } else {
            throw new IllegalStateException("Already completed");
        }
        return this;
    }

    @Override
    public ChannelPromise complete() {
        return complete(null);
    }
}
