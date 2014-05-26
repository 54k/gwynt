package io.gwynt.core;

import io.gwynt.core.concurrent.DefaultPromise;

public class DefaultChannelPromise extends DefaultPromise<Channel> implements ChannelPromise {

    private final Channel channel;
    private EventExecutor eventExecutor;

    public DefaultChannelPromise(Channel channel) {
        this(channel, null);
    }

    public DefaultChannelPromise(Channel channel, EventExecutor eventExecutor) {
        this.channel = channel;
        this.eventExecutor = eventExecutor;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    protected EventExecutor executor() {
        return eventExecutor != null ? eventExecutor : channel.eventLoop();
    }

    @Override
    public ChannelPromise complete(Throwable cause) {
        fail(cause);
        return this;
    }

    @Override
    public ChannelPromise complete() {
        complete(channel);
        return this;
    }

    @Override
    public ChannelPromise chainPromise(ChannelPromise channelPromise, ChannelPromise... channelPromises) {
        super.chainPromise(channelPromise, channelPromises);
        return this;
    }

    @Override
    public ChannelFuture addListener(ChannelFutureListener channelFutureListener, ChannelFutureListener... channelFutureListeners) {
        super.addListener(channelFutureListener, channelFutureListeners);
        return this;
    }
}
