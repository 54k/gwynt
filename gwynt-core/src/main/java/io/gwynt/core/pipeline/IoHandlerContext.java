package io.gwynt.core.pipeline;

import io.gwynt.core.IoHandler;
import io.gwynt.core.IoSession;

public interface IoHandlerContext {

    String getName();

    IoHandler getIoHandler();

    IoSession getIoSession();

    IoHandlerContext fireOnRegistered();

    IoHandlerContext fireOnUnregistered();

    IoHandlerContext fireOpen();

    IoHandlerContext fireMessageReceived(Object message);

    IoHandlerContext fireMessageSent(Object message);

    IoHandlerContext fireClosing();

    IoHandlerContext fireClose();

    IoHandlerContext fireExceptionCaught(Throwable e);

    boolean isRemoved();
}
