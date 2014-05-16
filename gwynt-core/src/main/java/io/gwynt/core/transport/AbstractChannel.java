package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.Handler;
import io.gwynt.core.exception.RegistrationException;
import io.gwynt.core.pipeline.DefaultPipeline;
import io.gwynt.core.scheduler.EventScheduler;

import java.net.InetSocketAddress;

public abstract class AbstractChannel implements Channel {

    private final Endpoint endpoint;
    private final DefaultPipeline pipeline;

    private volatile Channel parent;
    private volatile Object attachment;
    private volatile Dispatcher dispatcher;

    protected AbstractChannel(Endpoint endpoint) {
        this(null, endpoint);
    }

    protected AbstractChannel(Channel parent, Endpoint endpoint) {
        this.parent = parent;
        this.endpoint = endpoint;

        pipeline = new DefaultPipeline(this);
        for (Handler handler : endpoint.getHandlers()) {
            pipeline.addLast(handler);
        }
    }

    @Override
    public boolean isRegistered() {
        return dispatcher != null;
    }

    @Override
    public Object attach(Object attachment) {
        return this.attachment = attachment;
    }

    @Override
    public Object attachment() {
        return attachment;
    }

    @Override
    public DefaultPipeline pipeline() {
        return pipeline;
    }

    @Override
    public EventScheduler scheduler() {
        return endpoint.getScheduler();
    }

    @Override
    public ChannelFuture bind(InetSocketAddress address) {
        return unsafe().bind(address, newChannelPromise());
    }

    @Override
    public ChannelFuture connect(InetSocketAddress address) {
        return unsafe().connect(address, newChannelPromise());
    }

    @Override
    public ChannelFuture read() {
        ChannelPromise channelPromise = newChannelPromise();
        pipeline.fireRead(channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture write(Object message) {
        ChannelPromise channelPromise = newChannelPromise();
        pipeline.fireMessageSent(message, channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture close() {
        ChannelPromise channelPromise = newChannelPromise();
        pipeline.fireClosing(channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture closeFuture() {
        return unsafe().closeFuture();
    }

    @Override
    public ChannelFuture unregister() {
        return dispatcher.unregister(this, newChannelPromise());
    }

    @Override
    public ChannelFuture register(Dispatcher dispatcher) {
        if (!isDispatcherCompatible(dispatcher)) {
            throw new RegistrationException("dispatcher is not compatible");
        }

        return dispatcher.register(this, newChannelPromise());
    }

    protected abstract boolean isDispatcherCompatible(Dispatcher dispatcher);
}
