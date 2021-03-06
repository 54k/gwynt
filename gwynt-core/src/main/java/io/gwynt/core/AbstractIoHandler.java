package io.gwynt.core;

import io.gwynt.core.pipeline.IoHandlerContext;

public abstract class AbstractIoHandler<I, O> implements IoHandler<I, O> {

    @Override
    public void onHandlerAdded(IoHandlerContext context) {
    }

    @Override
    public void onHandlerRemoved(IoHandlerContext context) {
    }

    @Override
    public void onRegistered(IoHandlerContext context) {
        context.fireOnRegistered();
    }

    @Override
    public void onUnregistered(IoHandlerContext context) {
        context.fireOnUnregistered();
    }

    @Override
    public void onOpen(IoHandlerContext context) {
        context.fireOpen();
    }

    @Override
    public void onMessageReceived(IoHandlerContext context, I message) {
        context.fireMessageReceived(message);
    }

    @Override
    public void onMessageSent(IoHandlerContext context, O message) {
        context.fireMessageSent(message);
    }

    @Override
    public void onClosing(IoHandlerContext context) {
        context.fireClosing();
    }

    @Override
    public void onClose(IoHandlerContext context) {
        context.fireClose();
    }

    @Override
    public void onExceptionCaught(IoHandlerContext context, Throwable e) {
        context.fireExceptionCaught(e);
    }
}
