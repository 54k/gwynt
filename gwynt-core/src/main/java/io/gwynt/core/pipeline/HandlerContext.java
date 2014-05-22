package io.gwynt.core.pipeline;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Handler;

public interface HandlerContext {

    String name();

    Handler handler();

    Channel channel();

    void fireRegistered();

    void fireUnregistered();

    void fireOpen();

    ChannelFuture read();

    ChannelFuture read(ChannelPromise channelPromise);

    void fireMessageReceived(Object message);

    ChannelFuture write(Object message);

    ChannelFuture write(Object message, ChannelPromise channelPromise);

    ChannelFuture close();

    ChannelFuture close(ChannelPromise channelPromise);

    void fireClose();

    void fireExceptionCaught(Throwable e);

    boolean isRemoved();
}
