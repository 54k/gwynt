package io.gwynt.core;

import io.gwynt.core.scheduler.EventScheduler;
import io.gwynt.core.transport.AbstractNioChannel;
import io.gwynt.core.transport.NioEventLoop;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EndpointBootstrap implements Endpoint {

    protected EventScheduler eventLoop = new NioEventLoop();
    protected List<Handler> handlers = new ArrayList<>();
    protected ChannelFactory channelFactory = new DefaultChannelFactory();
    protected Class<? extends Channel> channelClazz;

    @Override
    public Endpoint addHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler");
        }

        handlers.add(handler);
        return this;
    }

    @Override
    public Endpoint removeHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler");
        }

        handlers.remove(handler);
        return this;
    }

    @Override
    public Iterable<Handler> getHandlers() {
        return Collections.unmodifiableList(handlers);
    }

    @Override
    public ChannelFactory getChannelFactory() {
        return channelFactory;
    }

    @Override
    public Endpoint setChannelFactory(ChannelFactory channelFactory) {
        if (channelFactory == null) {
            throw new IllegalArgumentException("connectionFactory");
        }

        this.channelFactory = channelFactory;
        return this;
    }

    @Override
    public EventScheduler getScheduler() {
        return eventLoop;
    }

    @Override
    public Endpoint setScheduler(EventScheduler eventScheduler) {
        if (eventScheduler == null) {
            throw new IllegalArgumentException("scheduler");
        }

        this.eventLoop = eventScheduler;
        return this;
    }

    @Override
    public Endpoint setChannelClass(Class<? extends Channel> channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel");
        }
        channelClazz = channel;
        return this;
    }

    @Override
    public Class<? extends Channel> getChannelClass() {
        return channelClazz;
    }

    @Override
    public ChannelFuture bind(final int port) {
        ChannelFuture regFuture = initAndRegisterChannel();
        regFuture.await();
        return regFuture.channel().bind(new InetSocketAddress(port));
    }

    @Override
    public ChannelFuture connect(final String host, final int port) {
        ChannelFuture regFuture = initAndRegisterChannel();
        regFuture.await();
        return regFuture.channel().connect(new InetSocketAddress(host, port));
    }

    @SuppressWarnings("unchecked")
    private ChannelFuture initAndRegisterChannel() {
        startEventLoop();
        Channel channel = channelFactory.createChannel(channelClazz);
        return channel.register(eventLoop);
    }

    private void startEventLoop() {
        eventLoop.runThread();
    }

    @Override
    public Endpoint shutdown() {
        eventLoop.shutdownThread();
        return this;
    }

    private class DefaultChannelFactory implements ChannelFactory<AbstractNioChannel> {

        @Override
        public AbstractNioChannel createChannel(Class<? extends AbstractNioChannel> channelClazz) {
            try {
                Constructor<? extends AbstractNioChannel> constructor = channelClazz.getConstructor(Endpoint.class);
                return constructor.newInstance(EndpointBootstrap.this);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
