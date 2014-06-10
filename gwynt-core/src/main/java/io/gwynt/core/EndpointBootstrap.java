package io.gwynt.core;

import io.gwynt.core.nio.AbstractNioChannel;
import io.gwynt.core.pipeline.HandlerContext;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EndpointBootstrap implements Endpoint {

    private EventLoopGroup primaryGroup;
    private EventLoopGroup secondaryGroup;

    private List<Handler> handlers = new ArrayList<>();
    private ChannelFactory channelFactory = new DefaultChannelFactory();
    private Class<? extends Channel> channelClazz;

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
    public Iterable<Handler> handlers() {
        return Collections.unmodifiableList(handlers);
    }

    @Override
    public ChannelFactory channelFactory() {
        return channelFactory;
    }

    @Override
    public Endpoint channelFactory(ChannelFactory channelFactory) {
        if (channelFactory == null) {
            throw new IllegalArgumentException("connectionFactory");
        }

        this.channelFactory = channelFactory;
        return this;
    }

    @Override
    public EventLoopGroup primaryGroup() {
        return primaryGroup;
    }

    @Override
    public EventLoopGroup secondaryGroup() {
        return secondaryGroup;
    }

    @Override
    public Endpoint group(EventLoopGroup group) {
        return group(group, group);
    }

    @Override
    public Endpoint group(EventLoopGroup primaryGroup, EventLoopGroup secondaryGroup) {
        if (primaryGroup == null) {
            throw new IllegalArgumentException("primaryGroup");
        }
        if (secondaryGroup == null) {
            throw new IllegalArgumentException("secondaryGroup");
        }

        this.primaryGroup = primaryGroup;
        this.secondaryGroup = secondaryGroup;
        return this;
    }

    @Override
    public Class<? extends Channel> channelClass() {
        return channelClazz;
    }

    @Override
    public Endpoint channelClass(Class<? extends Channel> channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel");
        }
        channelClazz = channel;
        return this;
    }

    @Override
    public Channel newChannel() {
        try {
            return initAndRegisterChannel().sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
            for (Handler handler : handlers()) {
                channel.pipeline().addLast(handler);
            }
        }
        return primaryGroup.register(channel);
    }

    @Override
    public Endpoint shutdown() {
        primaryGroup.shutdown();
        secondaryGroup.shutdown();
        return this;
    }

    private final class DefaultChannelAcceptor extends AbstractHandler<Channel, Object> {

        @Override
        public void onMessageReceived(HandlerContext context, Channel channel) {
            for (Handler handler : handlers()) {
                channel.pipeline().addLast(handler);
            }

            channel.register(secondaryGroup.next()).addListener(new ChannelFutureListener() {
                @Override
                public void onComplete(ChannelFuture channelFuture) {
                    if (channelFuture.channel().config().isAutoRead()) {
                        channelFuture.channel().read();
                    }
                }
            });
        }
    }

    private final class DefaultChannelFactory implements ChannelFactory<AbstractNioChannel> {

        @Override
        public AbstractNioChannel createChannel(Class<? extends AbstractNioChannel> channelClazz) {
            try {
                return channelClazz.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
