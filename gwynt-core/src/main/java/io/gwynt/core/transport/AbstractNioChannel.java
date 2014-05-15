package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.DefaultChannelFuture;
import io.gwynt.core.Endpoint;
import io.gwynt.core.Handler;
import io.gwynt.core.exception.EofException;
import io.gwynt.core.pipeline.DefaultPipeline;
import io.gwynt.core.scheduler.EventScheduler;
import io.gwynt.core.util.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractNioChannel implements Channel {

    protected static final ChannelFuture VOID_FUTURE = new DefaultChannelFuture(null);

    protected Unsafe unsafe;
    protected Endpoint endpoint;

    private Channel parent;
    private volatile Object attachment;
    private volatile Dispatcher dispatcher;

    private DefaultPipeline pipeline;

    protected AbstractNioChannel(Endpoint endpoint) {
        this(null, endpoint);
    }

    protected AbstractNioChannel(AbstractNioChannel parent, Endpoint endpoint) {
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
    public Unsafe unsafe() {
        return unsafe;
    }

    @Override
    public Dispatcher dispatcher() {
        return dispatcher;
    }

    @Override
    public Endpoint endpoint() {
        return endpoint;
    }

    @Override
    public ChannelFuture newChannelFuture() {
        return new DefaultChannelFuture(this);
    }

    protected abstract class AbstractUnsafe<T extends SelectableChannel> implements Unsafe<T> {

        private final Object lock = new Object();
        private volatile boolean pendingClose;
        private List<Object> messages = new ArrayList<>();
        private Queue<Pair<Object, ChannelFuture>> pendingWrites = new ConcurrentLinkedQueue<>();
        private T ch;

        protected AbstractUnsafe(T ch) {
            this.ch = ch;
            try {
                ch.configureBlocking(false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private volatile ChannelFuture closeFuture = VOID_FUTURE;

        @Override
        public T javaChannel() {
            return ch;
        }

        @Override
        public ChannelFuture bind(InetSocketAddress address) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture connect(InetSocketAddress address) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(Object message, ChannelFuture channelFuture) {
            if (!pendingClose && isActive()) {
                pendingWrites.add(new Pair<>(message, channelFuture));
                synchronized (lock) {
                    if (isRegistered()) {
                        dispatcher().modifyRegistration(AbstractNioChannel.this, SelectionKey.OP_WRITE);
                    }
                }
            }
        }

        @Override
        public void read() {
            if (!pendingClose && isActive()) {
                synchronized (lock) {
                    if (isRegistered()) {
                        dispatcher().modifyRegistration(AbstractNioChannel.this, SelectionKey.OP_READ);
                    }
                }
            }
        }

        @Override
        public void close(ChannelFuture channelFuture) {
            if (!pendingClose) {
                pendingClose = true;
                synchronized (lock) {
                    if (isRegistered()) {
                        closeFuture = channelFuture;
                        dispatcher().modifyRegistration(AbstractNioChannel.this, SelectionKey.OP_WRITE);
                    }
                }
            }
        }

        @Override
        public void doRegister(Dispatcher dispatcher) {
            synchronized (lock) {
                AbstractNioChannel.this.dispatcher = dispatcher;
                doRegister0();
            }
        }

        protected abstract void doRegister0();

        @Override
        public void doUnregister(Dispatcher dispatcher) {
            synchronized (lock) {
                AbstractNioChannel.this.dispatcher = null;
                doUnregister0();
            }
        }

        protected abstract void doUnregister0();

        @Override
        public void doAccept() throws IOException {
            List<Pair<AbstractNioChannel, ChannelFuture>> channels = new ArrayList<>();
            doAccept0(channels);
            for (Pair<AbstractNioChannel, ChannelFuture> pair : channels) {
                dispatcher().next().register(pair.getFirst(), pair.getSecond());
            }
        }

        protected abstract void doAccept0(List<Pair<AbstractNioChannel, ChannelFuture>> channels);

        @Override
        public void doRead() throws IOException {
            boolean shouldClose = pendingClose;
            try {
                doRead0(messages);
            } catch (EofException e) {
                shouldClose = true;
            }

            for (Object message : messages) {
                pipeline().fireMessageReceived(message);
            }
            messages.clear();

            if (shouldClose) {
                close0();
            }
        }

        protected abstract void doRead0(List<Object> messages);

        @Override
        public void doWrite() throws IOException {
            boolean shouldClose = pendingClose;
            Pair<Object, ChannelFuture> message = pendingWrites.peek();
            if (message != null) {
                try {
                    if (doWrite0(message.getFirst())) {
                        pendingWrites.poll();
                        message.getSecond().success();
                    }

                    if (!pendingWrites.isEmpty()) {
                        shouldClose = false;
                        dispatcher().modifyRegistration(AbstractNioChannel.this, SelectionKey.OP_WRITE);
                    }
                } catch (EofException e) {
                    shouldClose = true;
                    message.getSecond().success();
                }
            }

            if (shouldClose) {
                close0();
            }
        }

        protected abstract boolean doWrite0(Object message);

        @Override
        public void doConnect() throws IOException {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void exceptionCaught(Throwable e) {
            pipeline().fireExceptionCaught(e);
            close0();
        }

        protected boolean isActive() {
            return javaChannel().isOpen();
        }

        protected void close0() {
            pendingWrites.clear();
            try {
                ch.close();
            } catch (IOException e) {
                // ignore
            }
            dispatcher().unregister(AbstractNioChannel.this, closeFuture);
        }

    }
}
