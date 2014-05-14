package io.gwynt.core.pipeline;

import io.gwynt.core.Channel;
import io.gwynt.core.Handler;

public interface HandlerContext {

    String getName();

    Handler getHandler();

    Channel getChannel();

    HandlerContext fireRegistered();

    HandlerContext fireUnregistered();

    HandlerContext fireOpen();

    HandlerContext fireRead();

    HandlerContext fireMessageReceived(Object message);

    HandlerContext fireMessageSent(Object message);

    HandlerContext fireClosing();

    HandlerContext fireClose();

    HandlerContext fireExceptionCaught(Throwable e);

    boolean isRemoved();
}
