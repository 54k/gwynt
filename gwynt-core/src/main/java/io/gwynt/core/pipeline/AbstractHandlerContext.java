package io.gwynt.core.pipeline;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;

public abstract class AbstractHandlerContext implements HandlerContext {

    volatile Runnable registeredEvent;
    volatile Runnable unregisteredEvent;
    volatile Runnable openEvent;
    volatile Runnable closeEvent;
    volatile Runnable disconnectEvent;
    volatile Runnable readEvent;

    volatile boolean removed = true;

    private volatile AbstractHandlerContext prev;
    private volatile AbstractHandlerContext next;

    private HandlerContextInvoker invoker;
    private Channel channel;
    private String name;

    public AbstractHandlerContext(Channel channel) {
        this(null, channel);
    }

    public AbstractHandlerContext(HandlerContextInvoker invoker, Channel channel) {
        this.invoker = invoker;
        this.channel = channel;
    }

    @Override
    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AbstractHandlerContext getPrev() {
        return prev;
    }

    public void setPrev(AbstractHandlerContext prev) {
        this.prev = prev;
    }

    public AbstractHandlerContext getNext() {
        return next;
    }

    public void setNext(AbstractHandlerContext next) {
        this.next = next;
    }

    public HandlerContextInvoker invoker() {
        if (invoker == null) {
            return channel.eventLoop().asInvoker();
        }
        return invoker;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public void fireRegistered() {
        AbstractHandlerContext next = findContextInbound();
        next.invoker().invokeOnRegistered(next);
    }

    @Override
    public void fireUnregistered() {
        AbstractHandlerContext next = findContextInbound();
        next.invoker().invokeOnUnregistered(next);
    }

    @Override
    public void fireOpen() {
        AbstractHandlerContext next = findContextInbound();
        next.invoker().invokeOnOpen(next);
    }

    @Override
    public void fireMessageReceived(Object message) {
        AbstractHandlerContext next = findContextInbound();
        next.invoker().invokeOnMessageReceived(next, message);
    }

    @Override
    public void fireClose() {
        AbstractHandlerContext next = findContextInbound();
        next.invoker().invokeOnClosed(next);
    }

    @Override
    public void fireExceptionCaught(Throwable e) {
        AbstractHandlerContext next = findContextInbound();
        next.invoker().invokeOnExceptionCaught(next, e);
    }

    @Override
    public ChannelFuture read() {
        return read(channel.newChannelPromise());
    }

    @Override
    public ChannelFuture read(ChannelPromise channelPromise) {
        AbstractHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnRead(prev, channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture write(Object message) {
        return write(message, channel.newChannelPromise());
    }

    @Override
    public ChannelFuture write(Object message, ChannelPromise channelPromise) {
        AbstractHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnMessageSent(prev, message, channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture close() {
        return close(channel.newChannelPromise());
    }

    @Override
    public ChannelFuture close(ChannelPromise channelPromise) {
        AbstractHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnClosing(prev, channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture disconnect() {
        return disconnect(channel.newChannelPromise());
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise channelPromise) {
        AbstractHandlerContext prev = findContextOutbound();
        prev.invoker().invokeOnDisconnect(prev, channelPromise);
        return channelPromise;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

    private AbstractHandlerContext findContextInbound() {
        return next;
    }

    private AbstractHandlerContext findContextOutbound() {
        return prev;
    }

    @Override
    public String toString() {
        return getClass().getName() + "(name: " + name() + ", handler: " + handler().getClass().getName() + ')';
    }
}
