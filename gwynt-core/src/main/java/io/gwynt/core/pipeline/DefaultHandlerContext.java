package io.gwynt.core.pipeline;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Handler;

public final class DefaultHandlerContext implements HandlerContext {

    volatile Runnable registeredEvent;
    volatile Runnable unregisteredEvent;
    volatile Runnable openEvent;
    volatile Runnable closeEvent;
    volatile Runnable disconnectEvent;
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
            return channel.eventLoop().asInvoker();
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
    public void fireMessageReceived(Object message) {
        DefaultHandlerContext next = findContextInbound();
        next.invoker().invokeOnMessageReceived(next, message);
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
    public ChannelFuture read() {
        return read(channel.newChannelPromise());
    }

    @Override
    public ChannelFuture read(ChannelPromise channelPromise) {
        DefaultHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnRead(prev, channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture write(Object message) {
        return write(message, channel.newChannelPromise());
    }

    @Override
    public ChannelFuture write(Object message, ChannelPromise channelPromise) {
        DefaultHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnMessageSent(prev, message, channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture close() {
        return close(channel.newChannelPromise());
    }

    @Override
    public ChannelFuture close(ChannelPromise channelPromise) {
        DefaultHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnClosing(prev, channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture disconnect() {
        return disconnect(channel.newChannelPromise());
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise channelPromise) {
        DefaultHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnDisconnect(prev, channelPromise);
        return channelPromise;
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
