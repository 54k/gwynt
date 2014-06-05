package io.gwynt.core;

import io.gwynt.core.concurrent.EventExecutorGroup;

public interface EventLoopGroup extends EventExecutorGroup {

    @Override
    EventLoop next();

    ChannelFuture register(Channel channel);

    ChannelFuture register(Channel channel, ChannelPromise channelPromise);
}
