package io.gwynt.core.nio;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.ChannelException;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.EventLoop;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public abstract class AbstractNioChannel extends AbstractChannel {

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

        private final Runnable READ_TASK = new Runnable() {
            @Override
            public void run() {
                interestOps(interestOps() | SelectionKey.OP_READ);
            }
        };
        private final Runnable WRITE_TASK = new Runnable() {
            @Override
            public void run() {
                interestOps(interestOps() | SelectionKey.OP_WRITE);
            }
        };

        @Override
        protected void beforeClose() {
            // NO OP
        }

        @Override
        protected void writeRequested() {
            if (eventLoop().inExecutorThread()) {
                interestOps(interestOps() | SelectionKey.OP_WRITE);
            } else {
                invokeLater(WRITE_TASK);
            }
        }

        @Override
        protected void readRequested() {
            if (eventLoop().inExecutorThread()) {
                interestOps(interestOps() | SelectionKey.OP_READ);
            } else {
                invokeLater(READ_TASK);
            }
        }

        @Override
        public void doRead() {
            if (!config().isAutoRead()) {
                interestOps(interestOps() & ~SelectionKey.OP_READ);
            }
            super.doRead();
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
                throw new IllegalArgumentException("interestOps are not valid");
            }

            if (selectionKey.isValid()) {
                selectionKey.interestOps(interestOps);
                eventLoop().wakeUpSelector();
            } else {
                doClose();
            }
        }

        private void checkRegistered() {
            if (!isRegistered()) {
                throw new IllegalStateException("Not registered to dispatcher");
            }
        }
    }
}
