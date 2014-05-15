package io.gwynt.core;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface ChannelFuture extends Future<Channel> {

    Channel channel();

    void addListener(ChannelListener<? extends Channel> callback);

    void success();

    void fail(Throwable error);

    Channel await() throws Throwable;

    Channel await(long timeout, TimeUnit unit) throws Throwable;
}
