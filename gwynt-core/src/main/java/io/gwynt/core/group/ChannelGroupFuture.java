package io.gwynt.core.group;

import io.gwynt.core.ChannelFutureGroup;
import io.gwynt.core.concurrent.Future;
import io.gwynt.core.concurrent.FutureListener;

import java.util.concurrent.TimeUnit;

public interface ChannelGroupFuture extends ChannelFutureGroup {

    ChannelGroup group();

    @Override
    ChannelGroupFuture addListener(FutureListener<? extends Future<? super Void>> futureListener);

    @Override
    ChannelGroupFuture addListeners(FutureListener<? extends Future<? super Void>>... futureListeners);

    @Override
    ChannelGroupFuture removeListener(FutureListener<? extends Future<? super Void>> futureListener);

    @Override
    ChannelGroupFuture removeListeners(FutureListener<? extends Future<? super Void>>... futureListeners);

    @Override
    ChannelGroupFuture await() throws InterruptedException;

    @Override
    ChannelGroupFuture sync() throws InterruptedException;

    @Override
    ChannelGroupFuture sync(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    ChannelGroupFuture sync(long timeoutMillis) throws InterruptedException;
}
