package io.gwynt.core.rudp;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.EventLoop;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public abstract class AbstractVirtualChannel extends AbstractChannel {

    private static final int ST_ACTIVE = 1;
    private static final int ST_INACTIVE = 2;

    private static final AtomicIntegerFieldUpdater<AbstractVirtualChannel> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AbstractVirtualChannel.class, "state");

    @SuppressWarnings("FieldCanBeLocal")
    private volatile int state;

    protected AbstractVirtualChannel(Channel parent) {
        super(parent, null);
    }

    @Override
    protected boolean isEventLoopCompatible(EventLoop eventLoop) {
        return true;
    }

    public static interface VirtualUnsafe<T> extends Unsafe<T> {

        void messageReceived(Object message);
    }

    protected abstract class AbstractVirtualUnsafe<T> extends AbstractUnsafe<T> implements VirtualUnsafe<T> {

        private final ChannelFutureListener PARENT_LISTENER = new ChannelFutureListener() {
            @Override
            public void onComplete(ChannelFuture future) {
                close(voidPromise());
            }
        };

        private final Runnable CLOSE_TASK = new Runnable() {
            @Override
            public void run() {
                close(voidPromise());
            }
        };

        private final Runnable READ_TASK = new Runnable() {
            @Override
            public void run() {
                read();
            }
        };

        private final Runnable WRITE_TASK = new Runnable() {
            @Override
            public void run() {
                flush();
            }
        };

        protected abstract void read();

        @Override
        public boolean isActive() {
            return parent().isActive() && STATE_UPDATER.get(AbstractVirtualChannel.this) == ST_ACTIVE;
        }

        @Override
        public boolean isOpen() {
            return parent().unsafe().isOpen();
        }

        @Override
        protected void afterRegister() {
            parent().closeFuture().addListener(PARENT_LISTENER);
            STATE_UPDATER.set(AbstractVirtualChannel.this, ST_ACTIVE);
        }

        @Override
        protected void afterUnregister() {
            // NO OP
        }

        @Override
        public void closeForcibly() {
            parent().closeFuture().removeListener(PARENT_LISTENER);
            STATE_UPDATER.set(AbstractVirtualChannel.this, ST_INACTIVE);
        }
    }
}
