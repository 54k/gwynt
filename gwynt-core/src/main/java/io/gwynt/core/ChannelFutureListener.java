package io.gwynt.core;

public interface ChannelFutureListener {

    void onComplete(ChannelFuture channelFuture);

    void onError(ChannelFuture channelFuture, Throwable e);
}
