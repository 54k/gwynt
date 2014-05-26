package io.gwynt.core;

import io.gwynt.core.concurrent.Promise;

public interface ChannelPromise extends ChannelFuture, Promise<Channel> {

    ChannelPromise complete();

    ChannelPromise complete(Throwable error);

    ChannelPromise chainPromise(ChannelPromise channelPromise, ChannelPromise... channelPromises);
}
