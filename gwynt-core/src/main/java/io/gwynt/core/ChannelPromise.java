package io.gwynt.core;

public interface ChannelPromise extends ChannelFuture {

    ChannelPromise complete();

    ChannelPromise complete(Throwable error);

    ChannelPromise chainPromise(ChannelPromise channelPromise, ChannelPromise... channelPromises);
}
