package io.gwynt.core;

import java.util.concurrent.Future;

public interface ChannelFuture extends Future<Channel> {

    Channel channel();

    void addListener(ChannelListener<? extends Channel> callback);

    void complete();

    void complete(Throwable error);
}
