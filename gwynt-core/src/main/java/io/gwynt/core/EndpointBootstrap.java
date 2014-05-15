package io.gwynt.core;

import io.gwynt.core.scheduler.EventScheduler;
import io.gwynt.core.scheduler.SingleThreadedEventScheduler;
import io.gwynt.core.transport.AbstractNioChannel;
import io.gwynt.core.transport.Dispatcher;
import io.gwynt.core.transport.NioEventLoop;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class EndpointBootstrap implements Endpoint {

    protected Dispatcher eventLoop = new NioEventLoop();
    protected List<Handler> handlers = new ArrayList<>();
    protected ChannelFactory channelFactory = new DefaultChannelFactory();
    protected EventScheduler eventScheduler = new SingleThreadedEventScheduler();
    protected Class<? extends Channel> channelClazz;
    protected CountDownLatch latch = new CountDownLatch(1);

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
        return eventScheduler;
    }

    @Override
    public Endpoint setScheduler(EventScheduler eventScheduler) {
        if (eventScheduler == null) {
            throw new IllegalArgumentException("scheduler");
        }

        this.eventScheduler = eventScheduler;
        return this;
    }

    @Override
    public Dispatcher getDispatcher() {
        return eventLoop;
    }

    @Override
    public Endpoint setDispatcher(Dispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher");
        }
        this.eventLoop = dispatcher;
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
        ChannelFuture channelFuture = initAndRegisterChannel();
        channelFuture.addListener(new ChannelListener<Channel>() {
            @Override
            public void onComplete(Channel channel) {
                try {
                    channel.unsafe().bind(new InetSocketAddress(port)).await();
                    latch.countDown();
                } catch (Throwable ignore) {
                }
            }

            @Override
            public void onError(Channel channel, Throwable e) {
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return channelFuture;
    }

    @Override
    public ChannelFuture connect(final String host, final int port) {
        ChannelFuture channelFuture = initAndRegisterChannel();
        channelFuture.addListener(new ChannelListener<Channel>() {
            @Override
            public void onComplete(Channel channel) {
                try {
                    channel.unsafe().connect(new InetSocketAddress(host, port)).await();
                    latch.countDown();
                } catch (Throwable ignore) {
                }
            }

            @Override
            public void onError(Channel channel, Throwable e) {
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return channelFuture;
    }

    @SuppressWarnings("unchecked")
    private ChannelFuture initAndRegisterChannel() {
        startEventLoop();
        Channel channel = channelFactory.createChannel(channelClazz);
        return eventLoop.register(channel);
    }

    private void startEventLoop() {
        eventScheduler.runThread();
    }

    @Override
    public Endpoint shutdown() {
        eventScheduler.shutdownThread();
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
