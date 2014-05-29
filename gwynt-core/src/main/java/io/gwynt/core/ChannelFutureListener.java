package io.gwynt.core;

import io.gwynt.core.concurrent.FutureListener;

public interface ChannelFutureListener extends FutureListener<ChannelFuture> {

    static ChannelFutureListener CLOSE_LISTENER = new ChannelFutureListener() {
        @Override
        public void onComplete(ChannelFuture future) {
            future.channel().close();
        }
    };
}
