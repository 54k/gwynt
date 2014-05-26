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
    private volatile EventLoop eventLoop;
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
    public EventLoop eventLoop() {
        return eventLoop;
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
        return eventLoop.unregister(this, newChannelPromise());
    }

    @Override
    public ChannelFuture register(EventLoop eventLoop) {
        if (!isEventLoopCompatible(eventLoop)) {
            throw new RegistrationException("eventLoop is not compatible");
        }

        return eventLoop.register(this, newChannelPromise());
    }

    @Override
    public Object javaChannel() {
        return ch;
    }

    protected abstract boolean isEventLoopCompatible(EventLoop eventLoop);

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    protected abstract Unsafe newUnsafe();

    protected abstract class AbstractUnsafe<T> implements Unsafe<T> {

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
                readRequested();
                channelPromise.setSuccess();
            } else {
                channelPromise.setFailure(CLOSED_CHANNEL_EXCEPTION);
            }
        }

        protected abstract void readRequested();

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            if (!pendingClose && isActive()) {
                channelOutboundBuffer.addMessage(message, channelPromise);
                writeRequested();
            } else {
                channelPromise.setFailure(CLOSED_CHANNEL_EXCEPTION);
            }
        }

        protected abstract void writeRequested();

        protected abstract boolean isActive();

        @Override
        public void close(ChannelPromise channelPromise) {
            if (!pendingClose) {
                pendingClose = true;
                closeRequested();
            }
            closePromise.chainPromise(channelPromise);
        }

        protected abstract void closeRequested();

        @Override
        public void register(EventLoop eventScheduler) {
            assert eventScheduler.inExecutorThread();

            registered = true;
            AbstractChannel.this.eventLoop = eventScheduler;
            pipeline.fireRegistered();
            afterRegister();
        }

        protected abstract void afterRegister();

        @Override
        public void unregister() {
            assert eventLoop.inExecutorThread();

            registered = false;
            pipeline.fireUnregistered();
            afterUnregister();
        }

        protected abstract void afterUnregister();

        @Override
        public void doRead() throws IOException {
            assert eventLoop().inExecutorThread();

            int messagesRead = 0;
            for (int i = 0; i < config().getReadSpinCount(); i++) {
                int read = doReadMessages(messages);
                messagesRead += read;
                if (read == 0) {
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
            assert eventLoop().inExecutorThread();

            if (!isActive()) {
                channelOutboundBuffer.clear(CLOSED_CHANNEL_EXCEPTION);
                return;
            }

            if (!channelOutboundBuffer.isEmpty()) {
                doWriteMessages(channelOutboundBuffer);
            }

            if (channelOutboundBuffer.isEmpty() && pendingClose) {
                doClose();
            }
        }

        protected abstract void doWriteMessages(ChannelOutboundBuffer channelOutboundBuffer);

        @Override
        public void doConnect() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void exceptionCaught(final Throwable e) {
            invokeLater(new Runnable() {
                @Override
                public void run() {
                    pipeline().fireExceptionCaught(e);
                    doClose();
                }
            });
        }

        @Override
        public ChannelFuture closeFuture() {
            return closePromise;
        }

        protected void doClose() {
            assert eventLoop().inExecutorThread();

            if (!closePromise.isDone() && isActive()) {
                pendingClose = true;
                closeForcibly();
                channelOutboundBuffer.clear(CLOSED_CHANNEL_EXCEPTION);
                closePromise.setSuccess();
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!isActive()) {
                            pipeline.fireClose();
                        }

                        if (isRegistered()) {
                            unregister();
                        }
                    }
                });
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

        protected void invokeLater(Runnable task) {
            eventLoop().execute(task);
        }
    }
}
