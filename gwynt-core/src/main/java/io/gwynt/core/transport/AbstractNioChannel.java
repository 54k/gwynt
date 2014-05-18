package io.gwynt.core.transport;

import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.exception.ChannelException;
import io.gwynt.core.util.Pair;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;

public abstract class AbstractNioChannel extends AbstractChannel {

    private volatile SelectionKey selectionKey;
    private volatile Selector selector;

    protected AbstractNioChannel(Endpoint endpoint, SelectableChannel ch) {
        this(null, endpoint, ch);
    }

    protected AbstractNioChannel(AbstractNioChannel parent, Endpoint endpoint, SelectableChannel ch) {
        super(parent, endpoint, ch);
        try {
            ch.configureBlocking(false);
        } catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    protected boolean isDispatcherCompatible(Dispatcher dispatcher) {
        return dispatcher instanceof NioEventLoop;
    }

    protected abstract class AbstractNioUnsafe<T extends SelectableChannel> extends AbstractUnsafe<T> {

        @Override
        protected void writeRequested() {
            if (isRegistered()) {
                interestOps(interestOps() | SelectionKey.OP_WRITE);
            }
        }

        @Override
        protected void readRequested(ChannelPromise channelPromise) {
            if (isRegistered()) {
                interestOps(interestOps() | SelectionKey.OP_READ);
            }
        }

        @Override
        protected void writeMessages(Queue<Pair<Object, ChannelPromise>> messages) {
            Pair<Object, ChannelPromise> message = messages.peek();
            if (writeMessage(message.getFirst())) {
                messages.poll();
                message.getSecond().complete();
            }

            if (!messages.isEmpty()) {
                interestOps(interestOps() | SelectionKey.OP_WRITE);
            }
        }

        protected abstract boolean writeMessage(Object message);

        @Override
        protected void doAfterRegister() {
            try {
                selector = ((NioEventLoop) dispatcher()).selector;
                selectionKey = javaChannel().register(selector, 0, AbstractNioChannel.this);
            } catch (ClosedChannelException e) {
                throw new ChannelException(e);
            }
        }

        @Override
        protected void doAfterUnregister() {
            selector = null;
            selectionKey = null;
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

        protected int interestOps() {
            checkRegistered();
            return selectionKey.interestOps();
        }

        protected void interestOps(int interestOps) {
            checkRegistered();
            if ((interestOps & ~javaChannel().validOps()) != 0) {
                throw new IllegalArgumentException("interestOps are not valid");
            }
            selectionKey.interestOps(interestOps);
            selector.wakeup();
        }

        private void checkRegistered() {
            if (!isRegistered()) {
                throw new IllegalStateException("Not registered to dispatcher");
            }
        }
    }
}
