package io.gwynt.core;

import java.util.concurrent.TimeUnit;

public interface ChannelFuture {

    Channel channel();

    ChannelFuture addListener(ChannelFutureListener... callback);

    ChannelFuture await() throws Throwable;

    ChannelFuture await(long timeout, TimeUnit unit) throws Throwable;

    boolean isDone();
}
