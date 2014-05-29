package io.gwynt.core;

import io.gwynt.core.concurrent.EventExecutor;

public interface EventLoopGroup extends EventExecutor {

    EventLoop next();

    ChannelFuture register(Channel channel);

    ChannelFuture unregister(Channel channel);

    ChannelFuture register(Channel channel, ChannelPromise channelPromise);

    ChannelFuture unregister(Channel channel, ChannelPromise channelPromise);
}
