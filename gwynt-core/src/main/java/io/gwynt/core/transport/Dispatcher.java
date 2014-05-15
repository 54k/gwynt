package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;

public interface Dispatcher {

    Dispatcher next();

    ChannelFuture register(Channel channel);

    ChannelFuture unregister(Channel channel);

    ChannelFuture modifyRegistration(Channel channel, int interestOps);

    ChannelFuture register(Channel channel, ChannelPromise channelPromise);

    ChannelFuture unregister(Channel channel, ChannelPromise channelPromise);

    ChannelFuture modifyRegistration(Channel channel, int interestOps, ChannelPromise channelPromise);
}