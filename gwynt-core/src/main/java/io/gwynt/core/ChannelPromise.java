package io.gwynt.core;

import io.gwynt.core.concurrent.Promise;

public interface ChannelPromise extends ChannelFuture, Promise<Void> {

    ChannelPromise setSuccess();

    @Override
    ChannelPromise setSuccess(Void result);

    @Override
    ChannelPromise setFailure(Throwable error);

    @Override
    ChannelPromise chainPromise(Promise<Void>... promises);

    @Override
    ChannelPromise chainPromise(Promise<Void> promise);
}
