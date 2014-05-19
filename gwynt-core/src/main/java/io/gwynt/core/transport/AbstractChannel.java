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
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractChannel implements Channel {

    protected static final ChannelPromise VOID_PROMISE = new DefaultChannelPromise(null);
    protected static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();

    private final Endpoint endpoint;
    private final DefaultPipeline pipeline;

    private volatile Channel parent;
    private volatile Object attachment;
    private volatile Dispatcher dispatcher;

    private Object ch;
    private Unsafe unsafe;
    private SocketAddress localAddress;
    private SocketAddress remoteAddress;

    protected AbstractChannel(Channel parent, Endpoint endpoint, Object ch) {
        this.parent = parent;
        this.endpoint = endpoint;
        this.ch = ch;

        pipeline = new DefaultPipeline(this);
        for (Handler handler : endpoint.getHandlers()) {
            pipeline.addLast(handler);
        }
        unsafe = newUnsafe();
    }

    @Override
    public Channel parent() {
        return parent;
    }

    @Override
    public SocketAddress getLocalAddress() {
        if (localAddress == null) {
            try {
                return localAddress = unsafe().getLocalAddress();
            } catch (Exception ignore) {
                return null;
            }
        }
        return localAddress;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        if (remoteAddress == null) {
            try {
                return remoteAddress = unsafe().getRemoteAddress();
            } catch (Exception ignore) {
                return null;
            }
        }
        return remoteAddress;
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
        ChannelPromise channelPromise = newChannelPromise();
        unsafe().bind(address, channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture connect(InetSocketAddress address) {
        ChannelPromise channelPromise = newChannelPromise();
        unsafe().connect(address, channelPromise);
        return channelPromise;
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

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    protected abstract Unsafe newUnsafe();

    protected abstract class AbstractUnsafe<T> implements Unsafe<T> {

        private final Object registrationLock = new Object();
        private final ChannelPromise closePromise = newChannelPromise();
        private final List<Object> messages = new ArrayList<>();

        private volatile boolean pendingClose;
        private Queue<Pair<Object, ChannelPromise>> pendingWrites = new ConcurrentLinkedQueue<>();

        @SuppressWarnings("unchecked")
        @Override
        public T javaChannel() {
            return (T) ch;
        }

        @Override
        public void bind(InetSocketAddress address, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void connect(InetSocketAddress address, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void read(ChannelPromise channelPromise) {
            if (!pendingClose && isActive()) {
                synchronized (registrationLock) {
                    readRequested(channelPromise);
                }
            } else {
                channelPromise.complete(CLOSED_CHANNEL_EXCEPTION);
            }
        }

        protected abstract void readRequested(ChannelPromise channelPromise);

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            if (!pendingClose && isActive()) {
                pendingWrites.add(new Pair<>(message, channelPromise));
                synchronized (registrationLock) {
                    if (isRegistered()) {
                        writeRequested();
                    }
                }
            } else {
                channelPromise.complete(CLOSED_CHANNEL_EXCEPTION);
            }
        }

        protected abstract void writeRequested();

        protected abstract boolean isActive();

        @Override
        public void close(ChannelPromise channelPromise) {
            if (!pendingClose) {
                pendingClose = true;
                synchronized (registrationLock) {
                    if (isRegistered()) {
                        closeRequested();
                    }
                }
            }
            closePromise.chainPromise(channelPromise);
        }

        protected abstract void closeRequested();

        @Override
        public void register(Dispatcher dispatcher) {
            synchronized (registrationLock) {
                AbstractChannel.this.dispatcher = dispatcher;
                doAfterRegister();
                pipeline.fireRegistered();
            }
        }

        protected abstract void doAfterRegister();

        @Override
        public void unregister() {
            synchronized (registrationLock) {
                AbstractChannel.this.dispatcher = null;
                doAfterUnregister();
                pipeline.fireUnregistered();
            }
        }

        protected abstract void doAfterUnregister();

        @Override
        public void doAccept() throws IOException {
            List<Pair<Channel, ChannelPromise>> channels = new ArrayList<>();
            doAcceptChannels(channels);
            for (Pair<Channel, ChannelPromise> pair : channels) {
                dispatcher().next().register(pair.getFirst(), pair.getSecond());
            }
        }

        protected abstract void doAcceptChannels(List<Pair<Channel, ChannelPromise>> channels);

        @Override
        public void doRead() throws IOException {
            boolean shouldClose = pendingClose;
            try {
                doReadMessages(messages);
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

        protected abstract void doReadMessages(List<Object> messages);

        @Override
        public void doWrite() throws IOException {
            boolean shouldClose = pendingClose;
            try {
                if (!pendingWrites.isEmpty()) {
                    doWriteMessages(pendingWrites);
                    if (!pendingWrites.isEmpty()) {
                        shouldClose = false;
                    }
                }
            } catch (EofException e) {
                shouldClose = true;
                while (pendingWrites.peek() != null) {
                    pendingWrites.poll().getSecond().complete(e);
                }
            }

            if (shouldClose) {
                doClose();
            }
        }

        protected abstract void doWriteMessages(Queue<Pair<Object, ChannelPromise>> messages);

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
            if (!closePromise.isDone() && isActive()) {
                pendingClose = true;
                doCloseChannel();
                pendingWrites.clear();
                closePromise.complete();
                if (!isActive()) {
                    pipeline.fireClose();
                }
                if (isRegistered()) {
                    unregister();
                }
            }
        }

        protected abstract void doCloseChannel();

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return null;
        }

        @Override
        public SocketAddress getRemoteAddress() throws Exception {
            return null;
        }
    }
}
