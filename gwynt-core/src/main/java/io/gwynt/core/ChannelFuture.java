package io.gwynt.core;

import java.util.concurrent.TimeUnit;

public interface ChannelFuture {

    Channel channel();

    ChannelFuture addListener(ChannelFutureListener channelFutureListener, ChannelFutureListener... channelFutureListeners);

    ChannelFuture await();

    ChannelFuture await(long timeout, TimeUnit unit);

    boolean isDone();

    boolean isFailed();

    Throwable getCause();
}
