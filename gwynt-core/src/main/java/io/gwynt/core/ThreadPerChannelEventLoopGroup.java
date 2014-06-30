package io.gwynt.core;

import io.gwynt.core.concurrent.AbstractEventExecutorGroup;
import io.gwynt.core.concurrent.EventExecutor;
import io.gwynt.core.concurrent.Future;
import io.gwynt.core.concurrent.ThreadPerTaskExecutor;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class ThreadPerChannelEventLoopGroup extends AbstractEventExecutorGroup implements EventLoopGroup {

    final Set<EventLoop> activeChildren = Collections.newSetFromMap(new ConcurrentHashMap<EventLoop, Boolean>());
    private final Set<EventLoop> readOnlyActiveChildren = Collections.unmodifiableSet(activeChildren);
    final Queue<EventLoop> idleChildren = new ConcurrentLinkedQueue<>();
    final Executor executor;

    private final ChannelException tooManyChannels;
    private final int maxChannels;
    private final Object[] childArgs;

    protected ThreadPerChannelEventLoopGroup() {
        this(0);
    }

    protected ThreadPerChannelEventLoopGroup(int maxChannels) {
        this(maxChannels, Executors.defaultThreadFactory());
    }

    protected ThreadPerChannelEventLoopGroup(int maxChannels, ThreadFactory threadFactory, Object... args) {
        this(maxChannels, new ThreadPerTaskExecutor(threadFactory), args);
    }

    protected ThreadPerChannelEventLoopGroup(int maxChannels, Executor executor, Object... args) {
        if (maxChannels < 0) {
            throw new IllegalArgumentException(String.format("maxChannels: %d (expected: >= 0)", maxChannels));
        }
        if (executor == null) {
            throw new NullPointerException("executor");
        }

        if (args == null) {
            childArgs = new Object[0];
        } else {
            childArgs = args.clone();
        }

        this.maxChannels = maxChannels;
        this.executor = executor;

        tooManyChannels = new ChannelException("too many channels (max: " + maxChannels + ')');
        tooManyChannels.setStackTrace(new StackTraceElement[0]);
    }

    @Deprecated
    @Override
    public void shutdown() {
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
        return register(channel, channel.newChannelPromise());
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise channelPromise) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        try {
            return nextChild().register(channel, channelPromise);
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
        return null;
    }

    @Override
    public Future<Void> terminationFuture() {
        return null;
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
        return false;
    }

    protected EventLoop newChild(Object... args) throws Exception {
        return new ThreadPerChannelEventLoop(this);
    }

    private EventLoop nextChild() throws Exception {
        EventLoop loop = idleChildren.poll();
        if (loop == null) {
            if (maxChannels > 0 && activeChildren.size() >= maxChannels) {
                throw tooManyChannels;
            }
            loop = newChild(childArgs);
        }
        activeChildren.add(loop);
        return loop;
    }
}
