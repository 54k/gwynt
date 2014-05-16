package io.gwynt.core.pipeline;

import io.gwynt.core.ChannelPromise;

public interface HandlerContextInvoker {

    void invokeOnHandlerAdded(HandlerContext context);

    void invokeOnHandlerRemoved(HandlerContext context);

    void invokeOnRegistered(HandlerContext context);

    void invokeOnUnregistered(HandlerContext context);

    void invokeOnOpen(HandlerContext context);

    void invokeOnRead(HandlerContext context, ChannelPromise channelPromise);

    void invokeOnMessageReceived(HandlerContext context, Object message);

    void invokeOnMessageSent(HandlerContext context, Object message, ChannelPromise channelPromise);

    void invokeOnClosing(HandlerContext context, ChannelPromise channelPromise);

    void invokeOnClosed(HandlerContext context);

    void invokeOnExceptionCaught(HandlerContext context, Throwable e);
}
