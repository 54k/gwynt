package io.gwynt.core;

import io.gwynt.core.concurrent.DefaultFutureGroup;
import io.gwynt.core.concurrent.Future;
import io.gwynt.core.pipeline.HandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class Bootstrap implements Cloneable {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private EventLoopGroup primaryGroup;
    private EventLoopGroup secondaryGroup;
    private List<Handler> serverHandlers = new ArrayList<>();
    private List<Handler> childHandlers = new ArrayList<>();
    private ChannelFactory channelFactory = new DefaultChannelFactory();
    private Class<? extends Channel> channelClass;
    private Map<ChannelOption<Object>, Object> serverOptions = new HashMap<>();
    private Map<ChannelOption<Object>, Object> childOptions = new HashMap<>();

    public Bootstrap() {
    }

    private Bootstrap(Bootstrap b) {
        primaryGroup = b.primaryGroup;
        secondaryGroup = b.secondaryGroup;
        serverHandlers.addAll(b.serverHandlers);
        childHandlers.addAll(b.childHandlers);
        channelFactory = b.channelFactory;
        channelClass = b.channelClass;
        serverOptions.putAll(b.serverOptions);
        childOptions.putAll(b.childOptions);
    }

    public Bootstrap addServerHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler");
        }

        serverHandlers.add(handler);
        return this;
    }

    public Bootstrap removeServerHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler");
        }

        serverHandlers.remove(handler);
        return this;
    }

    public Bootstrap addChildHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler");
        }

        childHandlers.add(handler);
        return this;
    }

    public Bootstrap removeChildHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler");
        }

        childHandlers.remove(handler);
        return this;
    }

    public Iterable<Handler> serverHandlers() {
        return Collections.unmodifiableList(serverHandlers);
    }

    public Iterable<Handler> childHandlers() {
        return Collections.unmodifiableList(childHandlers);
    }

    public ChannelFactory channelFactory() {
        return channelFactory;
    }

    public Bootstrap channelFactory(ChannelFactory channelFactory) {
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

    public Bootstrap group(EventLoopGroup group) {
        return group(group, group);
    }

    public Bootstrap group(EventLoopGroup primaryGroup, EventLoopGroup secondaryGroup) {
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
        return channelClass;
    }

    public Bootstrap channelClass(Class<? extends Channel> channelClass) {
        if (channelClass == null) {
            throw new IllegalArgumentException("channelClass");
        }
        this.channelClass = channelClass;
        return this;
    }

    public Channel newChannel() {
        try {
            return initAndRegisterChannel().sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ChannelFuture bind(int port) {
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
        Channel channel = channelFactory.createChannel(channelClass);
        if (channel instanceof ServerChannel) {
            for (Handler handler : serverHandlers()) {
                channel.pipeline().addLast(handler);
            }

            for (Entry<ChannelOption<Object>, Object> e : childOptions.entrySet()) {
                if (!channel.config().setOption(e.getKey(), e.getValue())) {
                    logger.warn("Unknown server channel option: ", e.getKey());
                }
            }

            DefaultChannelAcceptor acceptor = new DefaultChannelAcceptor(childHandlers, secondaryGroup, childOptions);
            channel.pipeline().addLast(acceptor);
        } else {
            for (Handler handler : childHandlers()) {
                channel.pipeline().addLast(handler);
            }
        }
        return primaryGroup.register(channel);
    }

    @Deprecated
    public void shutdown() {
        primaryGroup.shutdown();
        secondaryGroup.shutdown();
    }

    public Future<Void> shutdownGracefully() {
        Collection<Future<Void>> futures = new ArrayList<>();
        futures.add(primaryGroup.shutdownGracefully());
        if (primaryGroup != secondaryGroup) {
            futures.add(secondaryGroup.shutdownGracefully());
        }

        return new DefaultFutureGroup<>(futures);
    }

    public Bootstrap childOption(ChannelOption<Object> channelOption, Object value) {
        if (channelOption == null) {
            throw new IllegalArgumentException("channelOption");
        }
        if (value == null) {
            throw new IllegalArgumentException("value");
        }
        childOptions.put(channelOption, value);
        return this;
    }

    public Bootstrap serverOption(ChannelOption<Object> channelOption, Object value) {
        if (channelOption == null) {
            throw new IllegalArgumentException("channelOption");
        }
        if (value == null) {
            throw new IllegalArgumentException("value");
        }
        serverOptions.put(channelOption, value);
        return this;
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Bootstrap clone() {
        return new Bootstrap(this);
    }

    private static final class DefaultChannelAcceptor extends AbstractHandler<Channel, Object> {

        private Iterable<Handler> childHandlers;
        private Map<ChannelOption<Object>, Object> childOptions;

        private EventLoopGroup secondaryGroup;

        private DefaultChannelAcceptor(Iterable<Handler> childHandlers, EventLoopGroup secondaryGroup, Map<ChannelOption<Object>, Object> childOptions) {
            this.childHandlers = childHandlers;
            this.secondaryGroup = secondaryGroup;
            this.childOptions = childOptions;
        }

        private static void handleException(Channel channel, Throwable t) {
            channel.unsafe().closeForcibly();
            logger.warn("Channel " + channel + " registration failed: ", t);
        }

        @Override
        public void onMessageReceived(HandlerContext context, final Channel channel) {
            for (Handler handler : childHandlers) {
                channel.pipeline().addLast(handler);
            }

            for (Entry<ChannelOption<Object>, Object> e : childOptions.entrySet()) {
                if (!channel.config().setOption(e.getKey(), e.getValue())) {
                    logger.warn("Unknown child channel option: ", e.getKey());
                }
            }

            try {
                secondaryGroup.register(channel).addListener(new ChannelFutureListener() {
                    @Override
                    public void onComplete(ChannelFuture channelFuture) {
                        Channel ch = channelFuture.channel();
                        if (channelFuture.isSuccess()) {
                            if (ch.config().isAutoRead()) {
                                ch.read();
                            }
                        } else {
                            handleException(channel, channelFuture.getCause());
                        }
                    }
                });
            } catch (Throwable t) {
                handleException(channel, t);
            }
        }
    }

    private static final class DefaultChannelFactory implements ChannelFactory<AbstractChannel> {
        @Override
        public AbstractChannel createChannel(Class<? extends AbstractChannel> channelClass) {
            try {
                return channelClass.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
