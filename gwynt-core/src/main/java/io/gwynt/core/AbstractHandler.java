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
        context.fireOnRegistered();
    }

    @Override
    public void onUnregistered(HandlerContext context) {
        context.fireOnUnregistered();
    }

    @Override
    public void onOpen(HandlerContext context) {
        context.fireOpen();
    }

    @Override
    public void onMessageReceived(HandlerContext context, I message) {
        context.fireMessageReceived(message);
    }

    @Override
    public void onMessageSent(HandlerContext context, O message) {
        context.fireMessageSent(message);
    }

    @Override
    public void onClosing(HandlerContext context) {
        context.fireClosing();
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
