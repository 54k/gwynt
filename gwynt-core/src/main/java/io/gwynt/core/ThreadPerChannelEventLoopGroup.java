package io.gwynt.core;

import io.gwynt.core.concurrent.AbstractEventExecutorGroup;
import io.gwynt.core.concurrent.EventExecutor;
import io.gwynt.core.concurrent.Future;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class ThreadPerChannelEventLoopGroup extends AbstractEventExecutorGroup implements EventLoopGroup {

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
}
