package io.gwynt.core;

import io.gwynt.core.exception.RegistrationException;
import io.gwynt.core.pipeline.DefaultPipeline;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractChannel implements Channel {

    protected static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();

    private final DefaultPipeline pipeline;

    private volatile Channel parent;
    private volatile Object attachment;
    private volatile EventScheduler eventScheduler;
    private volatile boolean registered;

    private Object ch;
    private Unsafe unsafe;
    private SocketAddress localAddress;
    private SocketAddress remoteAddress;
    private ChannelConfig config;

    protected AbstractChannel(Channel parent, Object ch) {
        this.parent = parent;
        this.ch = ch;

        pipeline = new DefaultPipeline(this);
        config = newConfig();
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
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    protected ChannelConfig newConfig() {
        return new DefaultChannelConfig(this);
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
        return eventScheduler;
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
        return eventScheduler.unregister(this, newChannelPromise());
    }

    @Override
    public ChannelFuture register(EventScheduler eventScheduler) {
        if (!isEventSchedulerCompatible(eventScheduler)) {
            throw new RegistrationException("eventScheduler is not compatible");
        }

        return eventScheduler.register(this, newChannelPromise());
    }

    protected abstract boolean isEventSchedulerCompatible(EventScheduler eventScheduler);

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    protected abstract Unsafe newUnsafe();

    protected abstract class AbstractUnsafe<T> implements Unsafe<T> {

        private final Object registrationLock = new Object();
        private final ChannelPromise closePromise = newChannelPromise();
        private final List<Object> messages = new ArrayList<>(config.getReadSpinCount());

        private volatile boolean pendingClose;
        private ChannelOutboundBuffer channelOutboundBuffer = newChannelOutboundBuffer();

        protected ChannelOutboundBuffer newChannelOutboundBuffer() {
            return new ChannelOutboundBuffer(AbstractChannel.this);
        }

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
                    readRequested();
                }
                channelPromise.complete();
            } else {
                channelPromise.complete(CLOSED_CHANNEL_EXCEPTION);
            }
        }

        protected abstract void readRequested();

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            if (!pendingClose && isActive()) {
                channelOutboundBuffer.addMessage(message, channelPromise);
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
        public void register(EventScheduler eventScheduler) {
            synchronized (registrationLock) {
                registered = true;
                AbstractChannel.this.eventScheduler = eventScheduler;
                pipeline.fireRegistered();
                afterRegister();
            }
        }

        protected abstract void afterRegister();

        @Override
        public void unregister() {
            synchronized (registrationLock) {
                registered = false;
                pipeline.fireUnregistered();
                afterUnregister();
            }
        }

        protected abstract void afterUnregister();

        @Override
        public void doRead() throws IOException {
            assert scheduler().inSchedulerThread();

            int messagesRead = 0;
            for (int i = 0; i < config().getReadSpinCount(); i++) {
                if ((messagesRead = doReadMessages(messages)) == 0) {
                    break;
                }
            }

            for (int i = 0; i < messagesRead; i++) {
                pipeline().fireMessageReceived(messages.get(i));
            }
            if (messagesRead > 0) {
                messages.clear();
            }
        }

        protected abstract int doReadMessages(List<Object> messages);

        @Override
        public void doWrite() throws IOException {
            assert scheduler().inSchedulerThread();

            if (!channelOutboundBuffer.isEmpty()) {
                int written = doWriteMessages(channelOutboundBuffer);
                for (int i = 0; i < written; i++) {
                    channelOutboundBuffer.remove();
                }
            }
        }

        protected abstract int doWriteMessages(ChannelOutboundBuffer channelOutboundBuffer);

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
            assert scheduler().inSchedulerThread();
            if (!closePromise.isDone() && isActive()) {
                pendingClose = true;
                closeForcibly();
                channelOutboundBuffer.clear();
                closePromise.complete();
                if (!isActive()) {
                    pipeline.fireClose();
                }

                if (isRegistered()) {
                    unregister();
                }
            }
        }

        protected abstract void closeForcibly();

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
