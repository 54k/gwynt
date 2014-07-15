package io.gwynt.core;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
    protected ChannelConfig newConfig() {
        return parent().config();
    }

    @Override
    protected boolean isEventLoopCompatible(EventLoop eventLoop) {
        return true;
    }

    @Override
    protected abstract AbstractVirtualUnsafe newUnsafe();

    @Override
    public VirtualUnsafe unsafe() {
        return (VirtualUnsafe) super.unsafe();
    }

    public static interface VirtualUnsafe<T> extends Unsafe<T> {

        void messageReceived(Object message);
    }

    protected abstract class AbstractVirtualUnsafe<T> extends AbstractUnsafe<T> implements VirtualUnsafe<T> {

        private final ChannelFutureListener parentListener = new ChannelFutureListener() {
            @Override
            public void onComplete(ChannelFuture future) {
                close(voidPromise());
            }
        };
        private final Runnable readTask = new Runnable() {
            @Override
            public void run() {
                readReceivedMessages();
            }
        };
        private final Runnable writeTask = new Runnable() {
            @Override
            public void run() {
                flush();
            }
        };

        @Override
        public void connect(InetSocketAddress address, ChannelPromise channelPromise) {
            safeSetFailure(channelPromise, new UnsupportedOperationException());
        }

        protected abstract void readReceivedMessages();

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
            parent().closeFuture().addListener(parentListener);
            STATE_UPDATER.set(AbstractVirtualChannel.this, ST_ACTIVE);
        }

        @Override
        protected void afterUnregister() {
            // NO OP
        }

        @Override
        public void closeForcibly() {
            parent().closeFuture().removeListener(parentListener);
            STATE_UPDATER.set(AbstractVirtualChannel.this, ST_INACTIVE);
        }

        @Override
        protected void readRequested() {
            if (eventLoop().inExecutorThread()) {
                readTask.run();
            } else {
                invokeLater(readTask);
            }
        }

        @Override
        protected void writeRequested() {
            if (eventLoop().inExecutorThread()) {
                writeTask.run();
            } else {
                invokeLater(writeTask);
            }
        }

        @Override
        protected void doWrite(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
            while (!channelOutboundBuffer.isEmpty()) {
                parent().unsafe().write(channelOutboundBuffer.current(), voidPromise());
                channelOutboundBuffer.remove();
            }
        }

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return parent().getLocalAddress();
        }

        @Override
        public SocketAddress getRemoteAddress() throws Exception {
            return parent().getRemoteAddress();
        }
    }
}
