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
        this(null, channel, handler);
    }

    public DefaultHandlerContext(HandlerContextInvoker invoker, Channel channel, Handler handler) {
        this.invoker = invoker;
        this.channel = channel;
        this.handler = handler;
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

    public HandlerContextInvoker invoker() {
        if (invoker == null) {
            return channel.scheduler().asInvoker();
        }
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
        invoker().invokeOnHandlerAdded(this);
    }

    public void fireOnRemoved() {
        invoker().invokeOnHandlerRemoved(this);
    }

    @Override
    public void fireRegistered() {
        DefaultHandlerContext next = findContextInbound();
        next.invoker().invokeOnRegistered(next);
    }

    @Override
    public void fireUnregistered() {
        DefaultHandlerContext next = findContextInbound();
        next.invoker().invokeOnUnregistered(next);
    }

    @Override
    public void fireOpen() {
        DefaultHandlerContext next = findContextInbound();
        next.invoker().invokeOnOpen(next);
    }

    @Override
    public void read() {
        read(channel.newChannelPromise());
    }

    @Override
    public void read(ChannelPromise channelPromise) {
        DefaultHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnRead(prev, channelPromise);
    }

    @Override
    public void fireMessageReceived(Object message) {
        DefaultHandlerContext next = findContextInbound();
        next.invoker().invokeOnMessageReceived(next, message);
    }

    @Override
    public void write(Object message) {
        write(message, channel.newChannelPromise());
    }

    @Override
    public void write(Object message, ChannelPromise channelPromise) {
        DefaultHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnMessageSent(prev, message, channelPromise);
    }

    @Override
    public void close() {
        close(channel.newChannelPromise());
    }

    @Override
    public void close(ChannelPromise channelPromise) {
        DefaultHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnClosing(prev, channelPromise);
    }

    @Override
    public void fireClose() {
        DefaultHandlerContext next = findContextInbound();
        next.invoker().invokeOnClosed(next);
    }

    @Override
    public void fireExceptionCaught(Throwable e) {
        DefaultHandlerContext next = findContextInbound();
        next.invoker().invokeOnExceptionCaught(next, e);
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
