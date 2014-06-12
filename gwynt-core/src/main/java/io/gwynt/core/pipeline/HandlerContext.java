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

    void fireMessageReceived(Object message);

    void fireClose();

    void fireExceptionCaught(Throwable e);

    ChannelFuture read();

    ChannelFuture read(ChannelPromise channelPromise);

    ChannelFuture write(Object message);

    ChannelFuture write(Object message, ChannelPromise channelPromise);

    ChannelFuture close();

    ChannelFuture close(ChannelPromise channelPromise);

    ChannelFuture disconnect();

    ChannelFuture disconnect(ChannelPromise channelPromise);

    boolean isRemoved();
}
