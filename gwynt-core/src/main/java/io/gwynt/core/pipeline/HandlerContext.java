package io.gwynt.core.pipeline;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Handler;

public interface HandlerContext {

    String name();

    Handler handler();

    Channel channel();

    void fireRegistered();

    void fireUnregistered();

    void fireOpen();

    void read();

    void read(ChannelPromise channelPromise);

    void fireMessageReceived(Object message);

    void write(Object message);

    void write(Object message, ChannelPromise channelPromise);

    void close();

    void close(ChannelPromise channelPromise);

    void fireClose();

    void fireExceptionCaught(Throwable e);

    boolean isRemoved();
}
