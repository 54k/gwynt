package io.gwynt.core;

import io.gwynt.core.concurrent.DefaultFutureGroup;
import io.gwynt.core.concurrent.Future;
import io.gwynt.core.concurrent.FutureGroup;
import io.gwynt.core.nio.AbstractNioChannel;
import io.gwynt.core.pipeline.HandlerContext;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class IOReactor {

    private EventLoopGroup primaryGroup;
    private EventLoopGroup secondaryGroup;

    private List<Handler> handlers = new ArrayList<>();
    private ChannelFactory channelFactory = new DefaultChannelFactory();
    private Class<? extends Channel> channelClazz;

    public IOReactor addHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler");
        }

        handlers.add(handler);
        return this;
    }

    public IOReactor removeHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler");
        }

        handlers.remove(handler);
        return this;
    }

    public Iterable<Handler> handlers() {
        return Collections.unmodifiableList(handlers);
    }

    public ChannelFactory channelFactory() {
        return channelFactory;
    }

    public IOReactor channelFactory(ChannelFactory channelFactory) {
        if (channelFactory == null) {
            throw new IllegalArgumentException("connectionFactory");
        }

        this.channelFactory = channelFactory;
        return this;
    }

    public EventLoopGroup primaryGroup() {
        return primaryGroup;
    }

    public EventLoopGroup secondaryGroup() {
        return secondaryGroup;
    }

    public IOReactor group(EventLoopGroup group) {
        return group(group, group);
    }

    public IOReactor group(EventLoopGroup primaryGroup, EventLoopGroup secondaryGroup) {
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

    public Class<? extends Channel> channelClass() {
        return channelClazz;
    }

    public IOReactor channelClass(Class<? extends Channel> channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel");
        }
        channelClazz = channel;
        return this;
    }

    public Channel newChannel() {
        try {
            return initAndRegisterChannel().sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ChannelFuture bind(final int port) {
        return newChannel().bind(new InetSocketAddress(port));
    }

    public ChannelFuture connect(String host, int port) {
        return connect(new InetSocketAddress(host, port));
    }

    public ChannelFuture connect(InetSocketAddress address) {
        return newChannel().connect(address);
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

    public FutureGroup<Void> shutdownGracefully() {
        Collection<Future<Void>> futures = new ArrayList<>();
        futures.add(primaryGroup.shutdownGracefully());
        if (primaryGroup != secondaryGroup) {
            futures.add(secondaryGroup.shutdownGracefully());
        }

        return new DefaultFutureGroup<>(futures);
    }

    private final class DefaultChannelAcceptor extends AbstractHandler<Channel, Object> {

        @Override
        public void onMessageReceived(final HandlerContext context, Channel channel) {
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
