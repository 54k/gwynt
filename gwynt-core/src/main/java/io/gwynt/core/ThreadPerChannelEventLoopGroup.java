package io.gwynt.core;

import io.gwynt.core.concurrent.AbstractEventExecutorGroup;
import io.gwynt.core.concurrent.DefaultPromise;
import io.gwynt.core.concurrent.EventExecutor;
import io.gwynt.core.concurrent.Future;
import io.gwynt.core.concurrent.FutureListener;
import io.gwynt.core.concurrent.GlobalEventExecutor;
import io.gwynt.core.concurrent.ThreadPerTaskExecutor;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class ThreadPerChannelEventLoopGroup extends AbstractEventExecutorGroup implements EventLoopGroup {

    final Set<EventLoop> activeChildren = Collections.newSetFromMap(new ConcurrentHashMap<EventLoop, Boolean>());
    private final Set<EventLoop> readOnlyActiveChildren = Collections.unmodifiableSet(activeChildren);
    final Queue<EventLoop> idleChildren = new ConcurrentLinkedQueue<>();
    final Executor executor;

    private final ChannelException tooManyChannels;
    private final int maxChannels;
    private final DefaultPromise<Void> shutdownFuture = new DefaultPromise<>();
    private final FutureListener<Future<Void>> shutdownListener = new FutureListener<Future<Void>>() {
        @Override
        public void onComplete(Future<Void> future) {
            if (isTerminated()) {
                shutdownFuture.trySuccess(null);
            }
        }
    };
    private volatile boolean shuttingDown;

    protected ThreadPerChannelEventLoopGroup() {
        this(0);
    }

    protected ThreadPerChannelEventLoopGroup(int maxChannels) {
        this(maxChannels, Executors.defaultThreadFactory());
    }

    protected ThreadPerChannelEventLoopGroup(int maxChannels, ThreadFactory threadFactory) {
        this(maxChannels, new ThreadPerTaskExecutor(threadFactory));
    }

    protected ThreadPerChannelEventLoopGroup(int maxChannels, Executor executor) {
        if (maxChannels < 0) {
            throw new IllegalArgumentException(String.format("maxChannels: %d (expected: >= 0)", maxChannels));
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }

        this.maxChannels = maxChannels;
        this.executor = executor;

        tooManyChannels = new ChannelException("too many channels (max: " + maxChannels + ')');
        tooManyChannels.setStackTrace(new StackTraceElement[0]);
    }

    @Deprecated
    @Override
    public void shutdown() {
        shuttingDown = true;

        for (EventLoop l : activeChildren) {
            l.shutdown();
        }

        for (EventLoop l : idleChildren) {
            l.shutdown();
        }
    }

    @Override
    public EventLoop next() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel");
        }
        try {
            return nextChild().register(channel);
        } catch (Throwable t) {
            ChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
            promise.setFailure(t);
            return promise;
        }
    }

    @Override
    public ChannelFuture register(ChannelPromise channelPromise) {
        if (channelPromise == null) {
            throw new IllegalArgumentException("channelPromise");
        }
        try {
            return nextChild().register(channelPromise);
        } catch (Throwable t) {
            channelPromise.setFailure(t);
            return channelPromise;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends EventExecutor> Set<E> children() {
        return (Set<E>) readOnlyActiveChildren;
    }

    @Override
    public Future<Void> shutdownGracefully() {
        shuttingDown = true;

        for (EventLoop l : activeChildren) {
            l.shutdownGracefully();
        }
        for (EventLoop l : idleChildren) {
            l.shutdownGracefully();
        }

        return shutdownFuture;
    }

    @Override
    public Future<Void> terminationFuture() {
        return shutdownFuture;
    }

    @Override
    public boolean isShutdown() {
        for (EventLoop l : activeChildren) {
            if (!l.isShutdown()) {
                return false;
            }
        }
        for (EventLoop l : idleChildren) {
            if (!l.isShutdown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTerminated() {
        for (EventLoop l : activeChildren) {
            if (!l.isTerminated()) {
                return false;
            }
        }
        for (EventLoop l : idleChildren) {
            if (!l.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return shutdownGracefully().await(timeout, unit);
    }

    protected EventLoop newChild() throws Exception {
        return new ThreadPerChannelEventLoop(this);
    }

    private EventLoop nextChild() throws Exception {
        if (shuttingDown) {
            throw new RejectedExecutionException("shutting down");
        }

        EventLoop loop = idleChildren.poll();
        if (loop == null) {
            if (maxChannels > 0 && activeChildren.size() >= maxChannels) {
                throw tooManyChannels;
            }
            loop = newChild();
            loop.terminationFuture().addListener(shutdownListener);
        }
        activeChildren.add(loop);
        return loop;
    }
}
