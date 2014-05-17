package io.gwynt.core.transport;

import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.exception.ChannelException;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public abstract class AbstractNioChannel extends AbstractChannel {

    protected AbstractNioUnsafe unsafe;

    protected AbstractNioChannel(Endpoint endpoint) {
        this(null, endpoint);
    }

    protected AbstractNioChannel(AbstractNioChannel parent, Endpoint endpoint) {
        super(parent, endpoint);
    }

    @Override
    protected boolean isDispatcherCompatible(Dispatcher dispatcher) {
        return dispatcher instanceof NioEventLoop;
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    protected abstract class AbstractNioUnsafe<T extends SelectableChannel> extends AbstractUnsafe<T> {

        protected AbstractNioUnsafe(T ch) {
            super(ch);
            try {
                ch.configureBlocking(false);
            } catch (IOException e) {
                throw new ChannelException(e);
            }
        }

        @Override
        protected void writeImpl() {
            if (isRegistered()) {
                dispatcher().modifyRegistration(AbstractNioChannel.this, SelectionKey.OP_WRITE);
            }
        }

        @Override
        protected void readImpl(ChannelPromise channelPromise) {
            if (isRegistered()) {
                dispatcher().modifyRegistration(AbstractNioChannel.this, SelectionKey.OP_READ, channelPromise);
            }
        }

        @Override
        protected boolean isActive() {
            return javaChannel().isOpen();
        }

        @Override
        protected void doCloseImpl() {
            try {
                javaChannel().close();
            } catch (IOException ignore) {
            }
        }
    }
}
