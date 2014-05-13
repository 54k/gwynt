package io.gwynt.core.pipeline;

public interface HandlerContextInvoker {

    void invokeOnHandlerAdded(HandlerContext context);

    void invokeOnHandlerRemoved(HandlerContext context);

    void invokeOnRegistered(HandlerContext context);

    void invokeOnUnregistered(HandlerContext context);

    void invokeOnOpen(HandlerContext context);

    void invokeOnMessageReceived(HandlerContext context, Object message);

    void invokeOnMessageSent(HandlerContext context, Object message);

    void invokeOnClosing(HandlerContext context);

    void invokeOnClosed(HandlerContext context);

    void invokeOnExceptionCaught(HandlerContext context, Throwable e);
}
