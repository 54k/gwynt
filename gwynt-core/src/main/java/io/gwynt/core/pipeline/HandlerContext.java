package io.gwynt.core.pipeline;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.Handler;

public interface HandlerContext {

    String name();

    Handler handler();

    Channel channel();

    void fireRegistered();

    void fireUnregistered();

    void fireOpen();

    void fireRead();

    void fireMessageReceived(Object message);

    void fireMessageSent(Object message);

    void fireMessageSent(Object message, ChannelFuture channelFuture);

    void fireClosing();

    void fireClosing(ChannelFuture channelFuture);

    void fireClose();

    void fireExceptionCaught(Throwable e);

    boolean isRemoved();
}
