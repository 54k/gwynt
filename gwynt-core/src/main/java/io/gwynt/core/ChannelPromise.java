package io.gwynt.core;

public interface ChannelPromise extends ChannelFuture {

    void complete();

    void complete(Throwable error);

    ChannelPromise chainPromise(ChannelPromise... channelPromise);
}
