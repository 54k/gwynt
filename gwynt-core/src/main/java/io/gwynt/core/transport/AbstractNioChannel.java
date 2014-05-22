package io.gwynt.core.transport;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.EventScheduler;
import io.gwynt.core.exception.ChannelException;
import io.gwynt.core.util.Pair;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Queue;

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
    protected boolean isEventSchedulerCompatible(EventScheduler eventScheduler) {
        return eventScheduler instanceof NioEventLoop;
    }

    protected abstract class AbstractNioUnsafe<T extends SelectableChannel> extends AbstractUnsafe<T> {

        @Override
        protected void writeRequested() {
            if (isRegistered()) {
                interestOps(interestOps() | SelectionKey.OP_WRITE);
            }
        }

        @Override
        protected void readRequested() {
            if (isRegistered()) {
                interestOps(interestOps() | SelectionKey.OP_READ);
            }
        }

        @Override
        protected void doWriteMessages(Queue<Pair<Object, ChannelPromise>> messages) {
            Pair<Object, ChannelPromise> message = messages.peek();
            if (doWriteMessage(message.getFirst())) {
                messages.poll();
                message.getSecond().complete();
            }

            if (!messages.isEmpty()) {
                interestOps(interestOps() | SelectionKey.OP_WRITE);
            }
        }

        protected abstract boolean doWriteMessage(Object message);

        @Override
        protected void doAfterRegister() {
            try {
                selectionKey = javaChannel().register(((NioEventLoop) scheduler()).selector, 0, AbstractNioChannel.this);
            } catch (ClosedChannelException e) {
                throw new ChannelException(e);
            }
        }

        @Override
        protected void doAfterUnregister() {
            selectionKey.cancel();
            selectionKey.attach(null);
        }

        @Override
        protected boolean isActive() {
            return javaChannel().isOpen();
        }

        @Override
        protected void doCloseChannel() {
            try {
                javaChannel().close();
            } catch (IOException ignore) {
            }
        }

        protected int interestOps() {
            checkRegistered();
            try {
                return selectionKey.interestOps();
            } catch (CancelledKeyException ignore) {
            }
            return 0;
        }

        protected void interestOps(int interestOps) {
            checkRegistered();
            if ((interestOps & ~javaChannel().validOps()) != 0) {
                throw new IllegalArgumentException("interestOps are not valid");
            }
            try {
                selectionKey.interestOps(interestOps);
                ((NioEventLoop) scheduler()).wakeUpSelector();
            } catch (CancelledKeyException ignore) {
            }
        }

        private void checkRegistered() {
            if (!isRegistered()) {
                throw new IllegalStateException("Not registered to dispatcher");
            }
        }
    }
}
