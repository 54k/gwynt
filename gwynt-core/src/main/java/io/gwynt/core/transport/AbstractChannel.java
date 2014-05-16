package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.DefaultChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.Handler;
import io.gwynt.core.exception.EofException;
import io.gwynt.core.exception.RegistrationException;
import io.gwynt.core.pipeline.DefaultPipeline;
import io.gwynt.core.scheduler.EventScheduler;
import io.gwynt.core.util.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    public Channel parent() {
        return parent;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public ChannelPromise newChannelPromise() {
        return new DefaultChannelPromise(this);
    }

    @Override
    public Endpoint endpoint() {
        return endpoint;
    }

    @Override
    public Dispatcher dispatcher() {
        return dispatcher;
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

    protected abstract class AbstractUnsafe<T> implements Unsafe<T> {

        private final Object lock = new Object();
        private final ChannelPromise closePromise = newChannelPromise();
        private final List<Object> messages = new ArrayList<>();

        private volatile boolean pendingClose;
        private Queue<Pair<Object, ChannelPromise>> pendingWrites = new ConcurrentLinkedQueue<>();
        private T ch;

        protected AbstractUnsafe(T ch) {
            this.ch = ch;
        }

        @Override
        public T javaChannel() {
            return ch;
        }

        @Override
        public ChannelFuture bind(InetSocketAddress address, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture connect(InetSocketAddress address, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void read(ChannelPromise channelPromise) {
            if (!pendingClose && isActive()) {
                synchronized (lock) {
                    if (isRegistered()) {
                        dispatcher().modifyRegistration(AbstractChannel.this, SelectionKey.OP_READ, channelPromise);
                    }
                }
            }
        }

        @Override
        public ChannelFuture write(Object message, ChannelPromise channelPromise) {
            if (!pendingClose && isActive()) {
                pendingWrites.add(new Pair<>(message, channelPromise));
                synchronized (lock) {
                    if (isRegistered()) {
                        dispatcher().modifyRegistration(AbstractChannel.this, SelectionKey.OP_WRITE);
                    }
                }
            }
            return channelPromise;
        }

        protected abstract boolean isActive();

        @Override
        public ChannelFuture close(ChannelPromise channelPromise) {
            if (!pendingClose) {
                pendingClose = true;
                synchronized (lock) {
                    if (isRegistered()) {
                        dispatcher().modifyRegistration(AbstractChannel.this, SelectionKey.OP_WRITE);
                    }
                }
            }
            closePromise.chainPromise(channelPromise);
            return channelPromise;
        }

        @Override
        public void doRegister(Dispatcher dispatcher) {
            synchronized (lock) {
                AbstractChannel.this.dispatcher = dispatcher;
                doAfterRegister();
            }
        }

        protected abstract void doAfterRegister();

        @Override
        public void doUnregister() {
            synchronized (lock) {
                AbstractChannel.this.dispatcher = null;
                doAfterUnregister();
            }
        }

        protected abstract void doAfterUnregister();

        @Override
        public void doAccept() throws IOException {
            List<Pair<AbstractNioChannel, ChannelPromise>> channels = new ArrayList<>();
            doAcceptImpl(channels);
            for (Pair<AbstractNioChannel, ChannelPromise> pair : channels) {
                dispatcher().next().register(pair.getFirst(), pair.getSecond());
            }
        }

        protected abstract void doAcceptImpl(List<Pair<AbstractNioChannel, ChannelPromise>> channels);

        @Override
        public void doRead() throws IOException {
            boolean shouldClose = pendingClose;
            try {
                doReadImpl(messages);
            } catch (EofException e) {
                shouldClose = true;
            }

            for (Object message : messages) {
                pipeline().fireMessageReceived(message);
            }
            messages.clear();

            if (shouldClose) {
                doClose();
            }
        }

        protected abstract void doReadImpl(List<Object> messages);

        @Override
        public void doWrite() throws IOException {
            boolean shouldClose = pendingClose;
            Pair<Object, ChannelPromise> message = pendingWrites.peek();
            if (message != null) {
                try {
                    if (doWriteImpl(message.getFirst())) {
                        pendingWrites.poll();
                        message.getSecond().complete();
                    }

                    if (!pendingWrites.isEmpty()) {
                        shouldClose = false;
                        dispatcher().modifyRegistration(AbstractChannel.this, SelectionKey.OP_WRITE);
                    }
                } catch (EofException e) {
                    shouldClose = true;
                    message.getSecond().complete(e);
                }
            }

            if (shouldClose) {
                doClose();
            }
        }

        protected abstract boolean doWriteImpl(Object message);

        @Override
        public void doConnect() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void exceptionCaught(Throwable e) {
            pipeline().fireExceptionCaught(e);
            doClose();
        }

        @Override
        public ChannelFuture closeFuture() {
            return closePromise;
        }

        protected void doClose() {
            pendingClose = true;
            doCloseImpl();
            pendingWrites.clear();
            if (isRegistered()) {
                unregister();
            }
        }

        protected abstract void doCloseImpl();
    }
}
