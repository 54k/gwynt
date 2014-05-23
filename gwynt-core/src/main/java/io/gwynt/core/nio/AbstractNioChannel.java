package io.gwynt.core.nio;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.EventLoop;
import io.gwynt.core.exception.ChannelException;

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
    protected boolean isEventLoopCompatible(EventLoop eventLoop) {
        return eventLoop instanceof NioEventLoop;
    }

    protected abstract class AbstractNioUnsafe<T extends SelectableChannel> extends AbstractUnsafe<T> {

        @Override
        protected void writeRequested() {
            invokeLater(new Runnable() {
                @Override
                public void run() {
                    interestOps(interestOps() | SelectionKey.OP_WRITE);
                }
            });
        }

        @Override
        protected void readRequested() {
            invokeLater(new Runnable() {
                @Override
                public void run() {
                    interestOps(interestOps() | SelectionKey.OP_READ);
                }
            });
        }

        @Override
        protected void doWriteMessages(ChannelOutboundBuffer channelOutboundBuffer) {
            for (; ; ) {
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
                    interestOps(interestOps() | SelectionKey.OP_WRITE);
                    break;
                }
            }
        }

        protected abstract boolean doWriteMessage(Object message);

        @Override
        protected void afterRegister() {
            try {
                selectionKey = javaChannel().register(((NioEventLoop) eventLoop()).selector, 0, AbstractNioChannel.this);
            } catch (ClosedChannelException e) {
                throw new ChannelException(e);
            }
        }

        @Override
        protected void afterUnregister() {
            selectionKey.cancel();
            selectionKey.attach(null);
        }

        @Override
        protected boolean isActive() {
            return javaChannel().isOpen();
        }

        @Override
        protected void closeForcibly() {
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
                ((NioEventLoop) eventLoop()).wakeUpSelector();
            }
        }

        private void checkRegistered() {
            if (!isRegistered()) {
                throw new IllegalStateException("Not registered to dispatcher");
            }
        }
    }
}
