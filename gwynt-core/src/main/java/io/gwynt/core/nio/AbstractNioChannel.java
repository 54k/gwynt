package io.gwynt.core.nio;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.ChannelException;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.EventLoop;
import io.gwynt.core.RegistrationException;
import io.gwynt.core.ServerChannel;
import io.gwynt.core.concurrent.ScheduledFuture;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class AbstractNioChannel extends AbstractChannel {

    protected int readOp = SelectionKey.OP_READ;
    private volatile SelectionKey selectionKey;

    protected AbstractNioChannel(SelectableChannel ch) {
        this(null, ch);
    }

    protected AbstractNioChannel(AbstractNioChannel parent, SelectableChannel ch) {
        super(parent, ch);
        try {
            ch.configureBlocking(false);
        } catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public NioEventLoop eventLoop() {
        return (NioEventLoop) super.eventLoop();
    }

    @Override
    protected boolean isEventLoopCompatible(EventLoop eventLoop) {
        return eventLoop instanceof NioEventLoop;
    }

    @Override
    public NioUnsafe unsafe() {
        return (NioUnsafe) super.unsafe();
    }

    public static interface NioUnsafe<T> extends Unsafe<T> {

        void read();

        void finishConnect();
    }

    protected abstract class AbstractNioUnsafe<T extends SelectableChannel> extends AbstractUnsafe<T> implements NioUnsafe<T> {

        private final List<Object> messages = new ArrayList<>(config().getReadSpinCount());
        private final Runnable WRITE_TASK = new Runnable() {
            @Override
            public void run() {
                interestOps(interestOps() | SelectionKey.OP_WRITE);
            }
        };
        private ChannelPromise connectPromise;
        private ScheduledFuture<?> connectTimeout;

        @Override
        protected void writeRequested() {
            if (eventLoop().inExecutorThread()) {
                WRITE_TASK.run();
            } else {
                invokeLater(WRITE_TASK);
            }
        }

        @Override
        protected void readRequested() {
            if (eventLoop().inExecutorThread()) {
                READ_TASK.run();
            } else {
                invokeLater(READ_TASK);
            }
        }

        @Override
        public void connect(final InetSocketAddress address, ChannelPromise channelPromise) {
            if (!channelPromise.setUncancellable()) {
                return;
            }

            try {
                boolean wasActive = isActive();
                if (doConnect(address, channelPromise)) {
                    safeSetSuccess(channelPromise);
                    if (!wasActive && isActive()) {
                        pipeline().fireOpen();
                        if (config().isAutoRead()) {
                            readRequested();
                        }
                    }
                } else {
                    connectPromise = channelPromise;
                    long connectTimeoutMillis = config().getConnectTimeoutMillis();
                    if (connectTimeoutMillis > 0) {
                        connectTimeout = eventLoop().schedule(new Runnable() {
                            @Override
                            public void run() {
                                ChannelException cause = new ChannelException("Connection timeout: " + address);
                                if (connectPromise != null && connectPromise.tryFailure(cause)) {
                                    closeForcibly();
                                }
                            }
                        }, config().getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
                    }

                    channelPromise.addListener(new ChannelFutureListener() {
                        @Override
                        public void onComplete(ChannelFuture future) {
                            if (future.isCancelled()) {
                                if (connectTimeout != null) {
                                    connectTimeout.cancel();
                                }
                                connectPromise = null;
                                closeForcibly();
                            }
                        }
                    });
                }
            } catch (Throwable t) {
                safeSetFailure(channelPromise, t);
                closeForcibly();
            }
        }

        protected abstract boolean doConnect(InetSocketAddress address, ChannelPromise channelPromise) throws Exception;

        @Override
        public void finishConnect() {
            boolean wasActive = isActive();
            try {
                if (doFinishConnect()) {
                    boolean connectSuccess = connectPromise.trySuccess();

                    if (!wasActive && isActive()) {
                        if (config().isAutoRead()) {
                            readRequested();
                        }
                        pipeline().fireOpen();
                    }

                    if (!connectSuccess) {
                        closeForcibly();
                    }
                } else {
                    closeForcibly();
                    connectPromise.tryFailure(new ChannelException("Connection failed"));
                }
            } catch (Throwable t) {
                safeSetFailure(connectPromise, t);
                closeForcibly();
            }
        }

        protected abstract boolean doFinishConnect() throws Exception;

        @Override
        public void read() {
            Throwable error = null;
            boolean closed = false;
            try {
                int messagesRead = 0;
                try {
                    for (int i = 0; i < config().getReadSpinCount(); i++) {
                        int read = doReadMessages(messages);
                        if (read == 0) {
                            break;
                        }
                        if (read < 0) {
                            closed = true;
                            break;
                        }
                        messagesRead += read;
                    }
                } catch (Throwable e) {
                    error = e;
                }

                for (int i = 0; i < messagesRead; i++) {
                    pipeline().fireMessageReceived(messages.get(i));
                }

                if (error != null) {
                    if (error instanceof IOException) {
                        closed = !(AbstractNioChannel.this instanceof ServerChannel);
                    }
                    pipeline().fireExceptionCaught(error);
                }

                if (closed && isOpen()) {
                    close(voidPromise());
                }
                messages.clear();
            } finally {
                if (isActive() && !config().isAutoRead()) {
                    removeReadOp();
                }
            }
        }

        /**
         * @return number of messages read or -1 if end of stream occurred
         */
        protected abstract int doReadMessages(List<Object> messages) throws Exception;

        @Override
        protected void doWrite(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
            boolean done = false;
            Object message = channelOutboundBuffer.current();
            if (message != null) {
                for (int i = 0; i < config().getWriteSpinCount(); i++) {
                    if (doWriteMessage(message)) {
                        done = true;
                        break;
                    }
                }
            }

            if (done) {
                channelOutboundBuffer.remove();
            } else {
                writeRequested();
            }
        }

        protected boolean doWriteMessage(Object message) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void afterRegister() {
            try {
                selectionKey = javaChannel().register(eventLoop().selector, 0, AbstractNioChannel.this);
            } catch (ClosedChannelException e) {
                throw new ChannelException(e);
            }
        }

        @Override
        protected void afterUnregister() {
            eventLoop().cancel(selectionKey);
        }

        @Override
        protected boolean isActive() {
            return javaChannel().isOpen();
        }

        @Override
        protected boolean isOpen() {
            return javaChannel().isOpen();
        }

        private void removeReadOp() {
            interestOps(interestOps() & ~readOp);
        }

        @Override
        public void closeForcibly() {
            try {
                javaChannel().close();
            } catch (IOException ignore) {
            }
        }

        protected int interestOps() {
            checkRegistered();
            if (selectionKey.isValid()) {
                return selectionKey.interestOps();
            }
            return 0;
        }

        protected void interestOps(int interestOps) {
            checkRegistered();
            if ((interestOps & ~javaChannel().validOps()) != 0) {
                throw new IllegalArgumentException("interestOps are not valid.");
            }

            if (selectionKey.isValid()) {
                selectionKey.interestOps(interestOps);
                eventLoop().wakeUpSelector();
            } else {
                closeForcibly();
            }
        }

        private void checkRegistered() {
            if (!isRegistered()) {
                throw new RegistrationException("Not registered to dispatcher.");
            }
        }

        private final Runnable READ_TASK = new Runnable() {
            @Override
            public void run() {
                interestOps(interestOps() | readOp);
            }
        };

    }
}
