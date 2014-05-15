package io.gwynt.core.pipeline;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Handler;

public class DefaultHandlerContext implements HandlerContext {

    volatile Runnable registeredEvent;
    volatile Runnable unregisteredEvent;
    volatile Runnable openEvent;
    volatile Runnable closeEvent;
    volatile Runnable readEvent;
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
    public String name() {
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
    public Handler handler() {
        return handler;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    public void fireOnAdded() {
        getInvoker().invokeOnHandlerAdded(this);
    }

    public void fireOnRemoved() {
        getInvoker().invokeOnHandlerRemoved(this);
    }

    @Override
    public void fireRegistered() {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnRegistered(next);
    }

    @Override
    public void fireUnregistered() {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnUnregistered(next);
    }

    @Override
    public void fireOpen() {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnOpen(next);
    }

    @Override
    public void fireRead() {
        DefaultHandlerContext prev = findContextOutbound();
        prev.getInvoker().invokeOnRead(prev);
    }

    @Override
    public void fireMessageReceived(Object message) {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnMessageReceived(next, message);
    }

    @Override
    public void fireMessageSent(Object message) {
        fireMessageSent(message, channel.newChannelPromise());
    }

    @Override
    public void fireMessageSent(Object message, ChannelPromise channelPromise) {
        DefaultHandlerContext prev = findContextOutbound();
        prev.getInvoker().invokeOnMessageSent(prev, message, channelPromise);
    }

    @Override
    public void fireClosing() {
        fireClosing(channel.newChannelPromise());
    }

    @Override
    public void fireClosing(ChannelPromise channelPromise) {
        DefaultHandlerContext prev = findContextOutbound();
        prev.getInvoker().invokeOnClosing(prev, channelPromise);
    }

    @Override
    public void fireClose() {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnClosed(next);
    }

    @Override
    public void fireExceptionCaught(Throwable e) {
        DefaultHandlerContext next = findContextInbound();
        next.getInvoker().invokeOnExceptionCaught(next, e);
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
