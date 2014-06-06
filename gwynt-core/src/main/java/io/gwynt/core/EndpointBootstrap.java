package io.gwynt.core;

import io.gwynt.core.nio.AbstractNioChannel;
import io.gwynt.core.nio.NioEventLoop;
import io.gwynt.core.pipeline.HandlerContext;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EndpointBootstrap implements Endpoint {

    protected EventLoopGroup eventLoop = new NioEventLoop();
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
    public EventLoopGroup getEventLoop() {
        return eventLoop;
    }

    @Override
    public Endpoint setEventLoop(EventLoopGroup eventLoop) {
        if (eventLoop == null) {
            throw new IllegalArgumentException("eventLoop");
        }

        this.eventLoop = eventLoop;
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
        try {
            regFuture.sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return regFuture.channel().bind(new InetSocketAddress(port));
    }

    @Override
    public ChannelFuture connect(String host, int port) {
        return connect(new InetSocketAddress(host, port));
    }

    @Override
    public ChannelFuture connect(InetSocketAddress address) {
        ChannelFuture regFuture = initAndRegisterChannel();
        try {
            regFuture.sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return regFuture.channel().connect(address);
    }

    @SuppressWarnings("unchecked")
    private ChannelFuture initAndRegisterChannel() {
        Channel channel = channelFactory.createChannel(channelClazz);
        if (channel instanceof ServerChannel) {
            channel.pipeline().addFirst(new DefaultChannelAcceptor());
        } else {
            for (Handler handler : getHandlers()) {
                channel.pipeline().addLast(handler);
            }
        }
        return eventLoop.register(channel);
    }

    @Override
    public Endpoint shutdown() {
        eventLoop.shutdown();
        return this;
    }

    private class DefaultChannelFactory implements ChannelFactory<AbstractNioChannel> {

        @Override
        public AbstractNioChannel createChannel(Class<? extends AbstractNioChannel> channelClazz) {
            try {
                return channelClazz.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class DefaultChannelAcceptor extends AbstractHandler<Channel, Object> {

        @Override
        public void onMessageReceived(HandlerContext context, Channel channel) {
            for (Handler handler : getHandlers()) {
                channel.pipeline().addLast(handler);
            }

            channel.register(eventLoop.next()).addListener(new ChannelFutureListener() {
                @Override
                public void onComplete(ChannelFuture channelFuture) {
                    if (channelFuture.channel().config().isAutoRead()) {
                        channelFuture.channel().read();
                    }
                }
            });
        }
    }
}
