package io.gwynt.core.transport;

import io.gwynt.core.*;
import io.gwynt.core.exception.ChannelException;
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

    protected static final ChannelPromise VOID_PROMISE = new DefaultChannelPromise(null);

    private final Endpoint endpoint;
    private final DefaultPipeline pipeline;

    private volatile Channel parent;
    private volatile Object attachment;
    private volatile Dispatcher dispatcher;

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

        private final Object registrationLock = new Object();
        private final ChannelPromise closePromise = newChannelPromise();
        private final List<Object> messages = new ArrayList<>();

        private volatile boolean pendingClose;
        private Queue<Pair<Object, ChannelPromise>> pendingWrites = new ConcurrentLinkedQueue<>();
        private T ch;

        protected AbstractUnsafe(T ch) {
            this.ch = ch;
        }

        protected ChannelPromise closePromise() {
            return closePromise;
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
        public ChannelFuture read(ChannelPromise channelPromise) {
            if (!pendingClose && isActive()) {
                synchronized (registrationLock) {
                    readImpl(channelPromise);
                }
            } else {
                channelPromise.complete(new ChannelException("Channel is closed"));
            }
            return channelPromise;
        }

        protected abstract void readImpl(ChannelPromise channelPromise);

        @Override
        public ChannelFuture write(Object message, ChannelPromise channelPromise) {
            if (!pendingClose && isActive()) {
                pendingWrites.add(new Pair<>(message, channelPromise));
                synchronized (registrationLock) {
                    writeImpl();
                }
            } else {
                channelPromise.complete(new ChannelException("Channel is closed"));
            }
            return channelPromise;
        }

        protected abstract void writeImpl();

        protected abstract boolean isActive();

        @Override
        public ChannelFuture close(ChannelPromise channelPromise) {
            if (!pendingClose) {
                pendingClose = true;
                synchronized (registrationLock) {
                    closeImpl();
                }
            }
            closePromise.chainPromise(channelPromise);
            return channelPromise;
        }

        protected abstract void closeImpl();

        @Override
        public void doRegister(Dispatcher dispatcher) {
            synchronized (registrationLock) {
                AbstractChannel.this.dispatcher = dispatcher;
                doAfterRegister();
            }
        }

        protected abstract void doAfterRegister();

        @Override
        public void doUnregister() {
            synchronized (registrationLock) {
                AbstractChannel.this.dispatcher = null;
                doAfterUnregister();
            }
        }

        protected abstract void doAfterUnregister();

        @Override
        public void doAccept() throws IOException {
            List<Pair<Channel, ChannelPromise>> channels = new ArrayList<>();
            doAcceptImpl(channels);
            for (Pair<Channel, ChannelPromise> pair : channels) {
                dispatcher().next().register(pair.getFirst(), pair.getSecond());
            }
        }

        protected abstract void doAcceptImpl(List<Pair<Channel, ChannelPromise>> channels);

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
            if (!closePromise.isDone()) {
                pendingClose = true;
                doCloseImpl();
                pendingWrites.clear();
                if (isRegistered()) {
                    unregister();
                }
            }
        }

        protected abstract void doCloseImpl();
    }
}
