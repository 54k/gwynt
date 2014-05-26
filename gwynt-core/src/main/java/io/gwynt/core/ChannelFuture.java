package io.gwynt.core;

import io.gwynt.core.concurrent.Future;

public interface ChannelFuture extends Future<Channel> {

    Channel channel();

    ChannelFuture addListener(ChannelFutureListener channelFutureListener, ChannelFutureListener... channelFutureListeners);
}
