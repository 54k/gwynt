package io.gwynt.core;

public interface ChannelPromise extends ChannelFuture {

    void success();

    void fail(Throwable error);
}
