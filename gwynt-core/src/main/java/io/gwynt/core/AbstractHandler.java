package io.gwynt.core;

import io.gwynt.core.pipeline.HandlerContext;

public abstract class AbstractHandler<I, O> implements Handler<I, O> {

    @Override
    public void onHandlerAdded(HandlerContext context) {
    }

    @Override
    public void onHandlerRemoved(HandlerContext context) {
    }

    @Override
    public void onRegistered(HandlerContext context) {
        context.fireRegistered();
    }

    @Override
    public void onUnregistered(HandlerContext context) {
        context.fireUnregistered();
    }

    @Override
    public void onOpen(HandlerContext context) {
        context.fireOpen();
    }

    @Override
    public void onRead(HandlerContext context) {
        context.fireRead();
    }

    @Override
    public void onMessageReceived(HandlerContext context, I message) {
        context.fireMessageReceived(message);
    }

    @Override
    public void onMessageSent(HandlerContext context, O message, ChannelFuture channelFuture) {
        context.fireMessageSent(message, channelFuture);
    }

    @Override
    public void onClosing(HandlerContext context, ChannelFuture channelFuture) {
        context.fireClosing(channelFuture);
    }

    @Override
    public void onClose(HandlerContext context) {
        context.fireClose();
    }

    @Override
    public void onExceptionCaught(HandlerContext context, Throwable e) {
        context.fireExceptionCaught(e);
    }
}
