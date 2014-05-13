package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.Endpoint;
import io.gwynt.core.Handler;
import io.gwynt.core.exception.EofException;
import io.gwynt.core.pipeline.DefaultPipeline;
import io.gwynt.core.scheduler.EventScheduler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractNioChannel implements Channel {

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

    protected abstract class AbstractUnsafe<T extends SelectableChannel> implements Unsafe<T> {

        private final Object lock = new Object();

        private volatile boolean active;

        private List<Object> messages = new ArrayList<>();
        private Queue<Object> pendingWrites = new ConcurrentLinkedQueue<>();
        private T ch;

        protected AbstractUnsafe(T ch) {
            this.ch = ch;
            try {
                ch.configureBlocking(false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public T javaChannel() {
            return ch;
        }

        @Override
        public void bind(InetSocketAddress address) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void connect(InetSocketAddress address) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(Object message) {
            if (active) {
                pendingWrites.add(message);
                synchronized (lock) {
                    if (isRegistered()) {
                        AbstractNioChannel.this.dispatcher.modifyRegistration(AbstractNioChannel.this, SelectionKey.OP_WRITE);
                    }
                }
            }
        }

        @Override
        public void read() {
            if (active) {
                synchronized (lock) {
                    if (isRegistered()) {
                        AbstractNioChannel.this.dispatcher.modifyRegistration(AbstractNioChannel.this, SelectionKey.OP_READ);
                    }
                }
            }
        }

        @Override
        public void close() {
            if (active) {
                active = false;
                synchronized (lock) {
                    if (isRegistered()) {
                        AbstractNioChannel.this.dispatcher.modifyRegistration(AbstractNioChannel.this, SelectionKey.OP_WRITE);
                    }
                }
            }
        }

        @Override
        public void doRegister(Dispatcher dispatcher) {
            synchronized (lock) {
                AbstractNioChannel.this.dispatcher = dispatcher;
                active = true;
                doRegister0(dispatcher);
            }
            AbstractNioChannel.this.pipeline.fireRegistered();
        }

        protected abstract void doRegister0(Dispatcher dispatcher);

        @Override
        public void doUnregister(Dispatcher dispatcher) {
            synchronized (lock) {
                AbstractNioChannel.this.dispatcher = null;
                active = false;
                doUnregister0(dispatcher);
            }
            AbstractNioChannel.this.pipeline.fireUnregistered();
        }

        protected abstract void doUnregister0(Dispatcher dispatcher);

        @Override
        public void doAccept() throws IOException {
            List<AbstractNioChannel> channels = new ArrayList<>();
            doAccept0(channels);
            for (AbstractNioChannel channel : channels) {
                AbstractNioChannel.this.dispatcher.next().register(channel);
            }
        }

        protected abstract void doAccept0(List<AbstractNioChannel> channels);

        @Override
        public void doRead() throws IOException {
            boolean shouldClose = !active;
            try {
                doRead0(messages);
            } catch (EofException e) {
                shouldClose = true;
            }

            for (Object message : messages) {
                AbstractNioChannel.this.pipeline.fireMessageReceived(message);
            }
            messages.clear();

            if (shouldClose) {
                close0();
            }
        }

        protected abstract void doRead0(List<Object> messages);

        @Override
        public void doWrite() throws IOException {
            boolean shouldClose = !active;
            Object message = pendingWrites.peek();
            if (message != null) {
                try {
                    if (doWrite0(message)) {
                        pendingWrites.poll();
                    }
                } catch (EofException e) {
                    shouldClose = true;
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
            AbstractNioChannel.this.pipeline.fireExceptionCaught(e);
            close0();
        }

        protected final boolean isActive() {
            return active;
        }

        protected void close0() {
            active = false;
            pendingWrites.clear();
            try {
                ch.close();
            } catch (IOException e) {
                // ignore
            }
            AbstractNioChannel.this.dispatcher.unregister(AbstractNioChannel.this);
        }
    }
}
