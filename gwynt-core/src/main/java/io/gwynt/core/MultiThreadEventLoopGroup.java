package io.gwynt.core;

import io.gwynt.core.concurrent.MultiThreadEventExecutorGroup;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public abstract class MultiThreadEventLoopGroup extends MultiThreadEventExecutorGroup implements EventLoopGroup {

    private static final int DEFAULT_NUM_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    protected MultiThreadEventLoopGroup() {
        this(DEFAULT_NUM_THREADS);
    }

    protected MultiThreadEventLoopGroup(ThreadFactory threadFactory) {
        super(DEFAULT_NUM_THREADS, threadFactory);
    }

    protected MultiThreadEventLoopGroup(Executor executor) {
        super(DEFAULT_NUM_THREADS, executor);
    }

    protected MultiThreadEventLoopGroup(int nThreads) {
        super(nThreads);
    }

    protected MultiThreadEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory);
    }

    protected MultiThreadEventLoopGroup(int nThreads, Executor executor) {
        super(nThreads, executor);
    }

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    protected abstract EventLoop newEventExecutor(Executor executor);

    @Override
    public ChannelFuture register(Channel channel) {
        return next().register(channel);
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise channelPromise) {
        return next().register(channel, channelPromise);
    }
}
