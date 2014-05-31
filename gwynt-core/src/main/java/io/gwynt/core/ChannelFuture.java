package io.gwynt.core;

import io.gwynt.core.concurrent.Future;
import io.gwynt.core.concurrent.FutureListener;

public interface ChannelFuture extends Future<Void> {

    Channel channel();

    @Override
    ChannelFuture addListener(FutureListener<? extends Future<? super Void>> futureListener);

    @Override
    ChannelFuture addListeners(FutureListener<? extends Future<? super Void>>... futureListeners);

    @Override
    ChannelFuture removeListener(FutureListener<? extends Future<? super Void>> futureListener);

    @Override
    ChannelFuture removeListeners(FutureListener<? extends Future<? super Void>>... futureListeners);
}
