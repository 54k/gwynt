package io.gwynt.core.nio;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;

public interface Dispatcher {

    Dispatcher parent();

    Dispatcher next();

    ChannelFuture register(Channel channel);

    ChannelFuture unregister(Channel channel);

    ChannelFuture register(Channel channel, ChannelPromise channelPromise);

    ChannelFuture unregister(Channel channel, ChannelPromise channelPromise);
}