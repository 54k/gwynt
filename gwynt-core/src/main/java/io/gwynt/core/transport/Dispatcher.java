package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;

public interface Dispatcher {

    Dispatcher next();

    ChannelFuture register(Channel channel);

    ChannelFuture unregister(Channel channel);

    ChannelFuture modifyRegistration(Channel channel, int interestOps);

    ChannelFuture register(Channel channel, ChannelFuture channelFuture);

    ChannelFuture unregister(Channel channel, ChannelFuture channelFuture);

    ChannelFuture modifyRegistration(Channel channel, int interestOps, ChannelFuture channelFuture);
}