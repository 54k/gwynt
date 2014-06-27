package io.gwynt.core;

import io.gwynt.core.concurrent.EventExecutor;
import io.gwynt.core.pipeline.DefaultPipeline;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractChannel implements Channel {

    protected static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();
    protected static final NotYetConnectedException NOT_YET_CONNECTED_EXCEPTION = new NotYetConnectedException();
    protected final ChannelPromise VOID_PROMISE = new VoidChannelPromise(this);

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

    protected static void safeSetSuccess(ChannelPromise channelPromise) {
        channelPromise.trySuccess();
    }

    protected static void safeSetFailure(ChannelPromise channelPromise, Throwable error) {
        channelPromise.tryFailure(error);
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
        return new DefaultChannelPromise(this, eventLoop());
    }

    @Override
    public ChannelPromise voidPromise() {
        return VOID_PROMISE;
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
    public ChannelFuture disconnect() {
        ChannelPromise channelPromise = newChannelPromise();
        unsafe().disconnect(channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture read() {
        ChannelPromise channelPromise = newChannelPromise();
        pipeline.fireRead(channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture write(Object message, ChannelPromise channelPromise) {
        pipeline.fireMessageSent(message, channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture write(Object message) {
        return write(message, newChannelPromise());
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
        return eventLoop.unregister(this);
    }

    @Override
    public ChannelFuture register(EventLoop eventLoop) {
        if (!isEventLoopCompatible(eventLoop)) {
            throw new RegistrationException("eventLoop is not compatible");
        }

        return eventLoop.register(this);
    }

    protected Object javaChannel() {
        return ch;
    }

    protected abstract boolean isEventLoopCompatible(EventLoop eventLoop);

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    protected abstract Unsafe newUnsafe();

    @Override
    public String toString() {
        return getClass().getName() + "(localAddress: " + getLocalAddress() + ", remoteAddress: " + getRemoteAddress() + ", pipeline: " + pipeline() + ')';
    }

    final class ClosePromise extends DefaultChannelPromise {

        ClosePromise(AbstractChannel ch) {
            super(ch);
        }

        @Override
        protected EventExecutor executor() {
            return isRegistered() ? eventLoop() : super.executor();
        }

        @Override
        public ChannelPromise setSuccess() {
            throw new IllegalStateException();
        }

        @Override
        public ChannelPromise setFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        @Override
        public boolean trySuccess() {
            throw new IllegalStateException();
        }

        @Override
        public boolean tryFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        boolean setClosed() {
            return super.trySuccess();
        }
    }

    protected abstract class AbstractUnsafe<T> implements Unsafe<T> {

        private final ClosePromise closePromise = new ClosePromise(AbstractChannel.this);
        private final List<Object> messages = new ArrayList<>(config.getReadSpinCount());

        private final AtomicBoolean pendingClose = new AtomicBoolean();
        private final AtomicBoolean inFlush = new AtomicBoolean();

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
        public void disconnect(ChannelPromise channelPromise) {
            if (!channelPromise.setUncancellable()) {
                return;
            }

            boolean wasActive = isActive();
            try {
                doDisconnect();
            } catch (Throwable t) {
                safeSetFailure(channelPromise, t);
                close(newChannelPromise());
                return;
            }

            if (wasActive && !isActive()) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireClose();
                    }
                });
            }

            safeSetSuccess(channelPromise);
            close(newChannelPromise());
        }

        protected void doDisconnect() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void read(ChannelPromise channelPromise) {
            if (!pendingClose.get() && isActive()) {
                readRequested();
                safeSetSuccess(channelPromise);
            } else {
                safeSetFailure(channelPromise, CLOSED_CHANNEL_EXCEPTION);
            }
        }

        protected abstract void readRequested();

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            if (!pendingClose.get() && isActive()) {
                channelOutboundBuffer.addMessage(message, channelPromise);
                writeRequested();
            } else {
                safeSetFailure(channelPromise, CLOSED_CHANNEL_EXCEPTION);
            }
        }

        protected abstract void writeRequested();

        protected abstract boolean isActive();

        protected abstract boolean isOpen();

        @Override
        public void close(ChannelPromise channelPromise) {
            if (pendingClose.compareAndSet(false, true)) {
                closeRequested();
                doClose();
            }
            closePromise.chainPromise(channelPromise);
        }

        protected abstract void closeRequested();

        @Override
        public void register(EventLoop eventScheduler) {
            assert eventScheduler.inExecutorThread();

            registered = true;
            AbstractChannel.this.eventLoop = eventScheduler;
            boolean registered = false;
            try {
                pipeline.fireRegistered();
                afterRegister();
                registered = true;
                if (isActive()) {
                    pipeline.fireOpen();
                }
            } finally {
                if (!registered) {
                    unregister();
                    closeJavaChannel();
                }
            }
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
        public void doRead() {
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
        public void doWrite() {
            assert eventLoop().inExecutorThread();

            if (!isActive()) {
                if (isOpen()) {
                    channelOutboundBuffer.clear(NOT_YET_CONNECTED_EXCEPTION);
                } else {
                    channelOutboundBuffer.clear(CLOSED_CHANNEL_EXCEPTION);
                }
                return;
            }

            if (!channelOutboundBuffer.isEmpty()) {
                try {
                    inFlush.set(true);
                    doWriteMessages(channelOutboundBuffer);
                } catch (Throwable e) {
                    channelOutboundBuffer.clear(e);
                } finally {
                    inFlush.set(false);
                }
            }
        }

        protected abstract void doWriteMessages(ChannelOutboundBuffer channelOutboundBuffer) throws Exception;

        @Override
        public void doConnect() {
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
            if (inFlush.get()) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        doClose();
                    }
                });
                return;
            }

            if (!closePromise.isDone()) {
                final boolean wasActive = isActive();
                pendingClose.set(true);
                closeJavaChannel();
                channelOutboundBuffer.clear(CLOSED_CHANNEL_EXCEPTION);
                closePromise.setClosed();

                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (wasActive && !isActive()) {
                            pipeline.fireClose();
                        }
                        unregister();
                    }
                });
            }
        }

        protected abstract void closeJavaChannel();

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return null;
        }

        @Override
        public SocketAddress getRemoteAddress() throws Exception {
            return null;
        }

        protected void invokeLater(Runnable task) {
            try {
                eventLoop().execute(task);
            } catch (RejectedExecutionException ignore) {
            }
        }
    }
}
