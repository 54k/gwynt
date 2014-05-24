package io.gwynt.core;

import io.gwynt.core.pipeline.HandlerContextInvoker;

public interface EventLoop extends EventLoopGroup, EventExecutor {

    HandlerContextInvoker asInvoker();

    ChannelFuture register(Channel channel);

    ChannelFuture unregister(Channel channel);

    ChannelFuture register(Channel channel, ChannelPromise channelPromise);

    ChannelFuture unregister(Channel channel, ChannelPromise channelPromise);
}
