package io.gwynt.core.pipeline;

import io.gwynt.core.ChannelFuture;

public interface HandlerContextInvoker {

    void invokeOnHandlerAdded(HandlerContext context);

    void invokeOnHandlerRemoved(HandlerContext context);

    void invokeOnRegistered(HandlerContext context);

    void invokeOnUnregistered(HandlerContext context);

    void invokeOnOpen(HandlerContext context);

    void invokeOnRead(HandlerContext context);

    void invokeOnMessageReceived(HandlerContext context, Object message);

    void invokeOnMessageSent(HandlerContext context, Object message, ChannelFuture channelFuture);

    void invokeOnClosing(HandlerContext context, ChannelFuture channelFuture);

    void invokeOnClosed(HandlerContext context);

    void invokeOnExceptionCaught(HandlerContext context, Throwable e);
}
