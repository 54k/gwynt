package io.gwynt.core.pipeline;

public interface IoHandlerInvoker {

    void invokeOnRegistered(IoHandlerContext context);

    void invokeOnUnregistered(IoHandlerContext context);

    void invokeOnOpen(IoHandlerContext context);

    void invokeOnMessageReceived(IoHandlerContext context, Object message);

    void invokeOnMessageSent(IoHandlerContext context, Object message);

    void invokeOnClosing(IoHandlerContext context);

    void invokeOnClosed(IoHandlerContext context);

    void invokeOnExceptionCaught(IoHandlerContext context, Throwable e);
}
