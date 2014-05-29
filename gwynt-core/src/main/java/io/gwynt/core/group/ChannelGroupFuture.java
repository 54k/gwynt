package io.gwynt.core.group;

import io.gwynt.core.ChannelFuture;
import io.gwynt.core.concurrent.Future;
import io.gwynt.core.concurrent.FutureListener;

public interface ChannelGroupFuture extends Future<Void>, Iterable<ChannelFuture> {

    ChannelGroup group();

    @Override
    ChannelGroupFuture addListener(FutureListener<? extends Future<? super Void>> futureListener);

    @Override
    ChannelGroupFuture addListeners(FutureListener<? extends Future<? super Void>>... futureListeners);

    @Override
    ChannelGroupFuture await() throws InterruptedException;
}
