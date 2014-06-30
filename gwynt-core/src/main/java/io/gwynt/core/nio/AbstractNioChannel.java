package io.gwynt.core.nio;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.ChannelException;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.EventLoop;
import io.gwynt.core.RegistrationException;
import io.gwynt.core.ServerChannel;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

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

    protected abstract class AbstractNioUnsafe<T extends SelectableChannel> extends AbstractUnsafe<T> {

        private final List<Object> messages = new ArrayList<>(config().getReadSpinCount());
        private final Runnable WRITE_TASK = new Runnable() {
            @Override
            public void run() {
                interestOps(interestOps() | SelectionKey.OP_WRITE);
            }
        };

        @Override
        protected void writeRequested() {
            if (eventLoop().inExecutorThread()) {
                WRITE_TASK.run();
            } else {
                invokeLater(WRITE_TASK);
            }
        }

        private final Runnable READ_TASK = new Runnable() {
            @Override
            public void run() {
                interestOps(interestOps() | readOp);
            }
        };

        @Override
        protected void readRequested() {
            if (eventLoop().inExecutorThread()) {
                READ_TASK.run();
            } else {
                invokeLater(READ_TASK);
            }
        }

        @Override
        public void doRead() {
            Throwable error = null;
            boolean closed = false;
            try {
                int messagesRead = 0;
                try {
                    for (int i = 0; i < config().getReadSpinCount(); i++) {
                        int read = doReadMessages(messages);
                        messagesRead += read;
                        if (read == 0) {
                            break;
                        }
                        if (read < 0) {
                            closed = true;
                            break;
                        }
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

        @Override
        protected void doWriteMessages(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
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
        protected void closeJavaChannel() {
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
                close(voidPromise());
            }
        }

        private void checkRegistered() {
            if (!isRegistered()) {
                throw new RegistrationException("Not registered to dispatcher.");
            }
        }


    }
}
