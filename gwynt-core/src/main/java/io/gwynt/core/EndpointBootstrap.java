package io.gwynt.core;

import io.gwynt.core.scheduler.EventScheduler;
import io.gwynt.core.scheduler.SingleThreadedEventScheduler;
import io.gwynt.core.transport.AbstractNioChannel;
import io.gwynt.core.transport.NioEventLoop;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class EndpointBootstrap implements Endpoint {

    protected NioEventLoop eventLoop = new NioEventLoop();
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
    public Endpoint setChannel(Class<? extends Channel> channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel");
        }
        channelClazz = channel;
        return this;
    }

    @Override
    public Class<? extends Channel> getChannel() {
        return channelClazz;
    }

    @Override
    public Endpoint bind(final int port) {
        initAndRegisterChannel(new ChannelCallback() {
            @Override
            public void onComplete(Channel channel) {
                channel.unsafe().bind(new InetSocketAddress(port));
                latch.countDown();
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
        return this;
    }

    @Override
    public Endpoint connect(final String host, final int port) {
        initAndRegisterChannel(new ChannelCallback() {
            @Override
            public void onComplete(Channel channel) {
                channel.unsafe().connect(new InetSocketAddress(host, port));
                latch.countDown();
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
        return this;
    }

    @SuppressWarnings("unchecked")
    private Channel initAndRegisterChannel(ChannelCallback callback) {
        startEventLoop();
        Channel channel = channelFactory.createChannel(channelClazz);
        eventLoop.register(channel, callback);
        return channel;
    }

    private void startEventLoop() {
        eventLoop.runThread();
        eventScheduler.runThread();
    }

    @Override
    public Endpoint unbind() {
        eventScheduler.shutdownThread();
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
