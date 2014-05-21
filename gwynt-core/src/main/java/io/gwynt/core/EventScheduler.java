package io.gwynt.core;

import io.gwynt.core.pipeline.HandlerContextInvoker;

public interface EventScheduler extends Runnable {

    HandlerContextInvoker asInvoker();

    boolean inSchedulerThread();

    ChannelFuture register(Channel channel);

    ChannelFuture unregister(Channel channel);

    ChannelFuture register(Channel channel, ChannelPromise channelPromise);

    ChannelFuture unregister(Channel channel, ChannelPromise channelPromise);

    void runThread();

    void shutdownThread();

    EventScheduler next();

    EventScheduler parent();

    void schedule(Runnable task);

    boolean isRunning();
}
