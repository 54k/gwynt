package io.gwynt.core;

import io.gwynt.core.concurrent.AbstractEventExecutorGroup;
import io.gwynt.core.concurrent.EventExecutor;
import io.gwynt.core.concurrent.Future;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public abstract class ThreadPerChannelEventLoopGroup extends AbstractEventExecutorGroup implements EventLoopGroup {

    private static final ChannelException tooManyChannels = new ChannelException("Too many channels");

    static {
        tooManyChannels.setStackTrace(new StackTraceElement[0]);
    }

    private final int maxChannels;

    Set<EventLoop> activeChildren = Collections.newSetFromMap(new ConcurrentHashMap<EventLoop, Boolean>());
    Queue<EventLoop> idleChildren = new ConcurrentLinkedQueue<>();
    Executor executor;

    private Object[] childArgs;

    protected ThreadPerChannelEventLoopGroup(int maxChannels) {
        this.maxChannels = maxChannels;
    }

    @Deprecated
    @Override
    public void shutdown() {
    }

    @Override
    public EventLoop next() {
        return null;
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return null;
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public <E extends EventExecutor> Set<E> children() {
        return null;
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
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    protected EventLoop newChild(@SuppressWarnings("UnusedParameters") Object... args) throws Exception {
        return new ThreadPerChannelEventLoop();
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
