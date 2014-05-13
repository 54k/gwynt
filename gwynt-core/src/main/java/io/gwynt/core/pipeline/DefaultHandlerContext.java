package io.gwynt.core.pipeline;

import io.gwynt.core.Channel;
import io.gwynt.core.Handler;

public class DefaultHandlerContext implements HandlerContext {

    volatile Runnable registeredEvent;
    volatile Runnable unregisteredEvent;
    volatile Runnable openEvent;
    volatile Runnable closeEvent;
    volatile boolean removed = true;

    private volatile DefaultHandlerContext prev;
    private volatile DefaultHandlerContext next;
    private HandlerContextInvoker invoker;
    private Channel channel;
    private Handler handler;
    private String name;

    public DefaultHandlerContext(Channel channel, Handler handler) {
        this.channel = channel;
        this.handler = handler;
        invoker = new DefaultHandlerContextInvoker(channel.scheduler());
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DefaultHandlerContext getPrev() {
        return prev;
    }

    public void setPrev(DefaultHandlerContext prev) {
        this.prev = prev;
    }

    public DefaultHandlerContext getNext() {
        return next;
    }

    public void setNext(DefaultHandlerContext next) {
        this.next = next;
    }

    public HandlerContextInvoker getInvoker() {
        return invoker;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    public void fireOnAdded() {
        getInvoker().invokeOnHandlerAdded(this);
    }

    public void fireOnRemoved() {
        getInvoker().invokeOnHandlerRemoved(this);
    }

    @Override
    public HandlerContext fireOnRegistered() {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnRegistered(next);
        return this;
    }

    @Override
    public HandlerContext fireOnUnregistered() {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnUnregistered(next);
        return this;
    }

    @Override
    public HandlerContext fireOpen() {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnOpen(next);
        return this;
    }

    @Override
    public HandlerContext fireMessageReceived(Object message) {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnMessageReceived(next, message);
        return this;
    }

    @Override
    public HandlerContext fireMessageSent(Object message) {
        DefaultHandlerContext prev = findContextOutbound();
        prev.getInvoker().invokeOnMessageSent(prev, message);
        return this;
    }

    @Override
    public HandlerContext fireClosing() {
        DefaultHandlerContext prev = findContextOutbound();
        prev.getInvoker().invokeOnClosing(prev);
        return this;
    }

    @Override
    public HandlerContext fireClose() {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnClosed(next);
        return this;
    }

    @Override
    public HandlerContext fireExceptionCaught(Throwable e) {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnExceptionCaught(next, e);
        return this;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

    private DefaultHandlerContext findContextInbound() {
        return next;
    }

    private DefaultHandlerContext findContextOutbound() {
        return prev;
    }
}
