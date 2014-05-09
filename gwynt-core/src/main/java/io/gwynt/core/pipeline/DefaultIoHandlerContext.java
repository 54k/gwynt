package io.gwynt.core.pipeline;

import io.gwynt.core.AbstractIoSession;
import io.gwynt.core.IoHandler;
import io.gwynt.core.IoSession;

public class DefaultIoHandlerContext implements IoHandlerContext {

    volatile Runnable openEvent;
    volatile Runnable closeEvent;

    private volatile DefaultIoHandlerContext prev;
    private volatile DefaultIoHandlerContext next;
    private IoHandlerInvoker invoker;
    private AbstractIoSession ioSession;
    private IoHandler ioHandler;

    public DefaultIoHandlerContext(AbstractIoSession ioSession, IoHandler ioHandler) {
        this.ioSession = ioSession;
        this.ioHandler = ioHandler;
        invoker = new DefaultIoHandlerInvoker(ioSession.getEndpoint().getScheduler());
    }

    public DefaultIoHandlerContext getPrev() {
        return prev;
    }

    public void setPrev(DefaultIoHandlerContext prev) {
        this.prev = prev;
    }

    public DefaultIoHandlerContext getNext() {
        return next;
    }

    public void setNext(DefaultIoHandlerContext next) {
        this.next = next;
    }

    public IoHandlerInvoker getInvoker() {
        return invoker;
    }

    @Override
    public IoHandler getIoHandler() {
        return ioHandler;
    }

    @Override
    public IoSession getIoSession() {
        return ioSession;
    }

    @Override
    public IoHandlerContext fireOpen() {
        DefaultIoHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnOpen(next);
        return this;
    }

    @Override
    public IoHandlerContext fireMessageReceived(Object message) {
        DefaultIoHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnMessageReceived(next, message);
        return this;
    }

    @Override
    public IoHandlerContext fireMessageSent(Object message) {
        DefaultIoHandlerContext prev = findContextOutbound();
        prev.getInvoker().invokeOnMessageSent(prev, message);
        return this;
    }

    @Override
    public IoHandlerContext fireClosing() {
        DefaultIoHandlerContext prev = findContextOutbound();
        prev.getInvoker().invokeOnClosing(prev);
        return this;
    }

    @Override
    public IoHandlerContext fireClose() {
        DefaultIoHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnClosed(next);
        return this;
    }

    @Override
    public IoHandlerContext fireExceptionCaught(Throwable e) {
        DefaultIoHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnExceptionCaught(next, e);
        return this;
    }

    private DefaultIoHandlerContext findContextInbound() {
        return next;
    }

    private DefaultIoHandlerContext findContextOutbound() {
        return prev;
    }
}
