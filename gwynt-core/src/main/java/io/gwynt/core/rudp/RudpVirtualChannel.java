package io.gwynt.core.rudp;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Datagram;
import io.gwynt.core.EventLoop;
import io.gwynt.core.buffer.ByteBufferPool;
import io.gwynt.core.buffer.DynamicByteBuffer;
import io.gwynt.core.concurrent.ScheduledFuture;
import io.gwynt.core.util.Buffers;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class RudpVirtualChannel extends AbstractChannel {

    private static final int ST_ACTIVE = 1;
    private static final int ST_INACTIVE = 2;
    private static final AtomicIntegerFieldUpdater<RudpVirtualChannel> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(RudpVirtualChannel.class, "state");

    SocketAddress remoteAddress;
    AtomicInteger localSeq = new AtomicInteger();
    AtomicInteger remoteSeq = new AtomicInteger();

    @SuppressWarnings("FieldCanBeLocal")
    private volatile int state;

    public RudpVirtualChannel(Channel parent) {
        super(parent, parent.unsafe().javaChannel());
    }

    @Override
    public NioRudpServerChannel parent() {
        return (NioRudpServerChannel) super.parent();
    }

    @Override
    protected boolean isEventLoopCompatible(EventLoop eventLoop) {
        return true;
    }

    @Override
    protected Unsafe newUnsafe() {
        return new RudpVirtualChannelUnsafe();
    }

    @Override
    protected ChannelConfig newConfig() {
        return parent().config();
    }

    @Override
    public RudpChannelConfig config() {
        return (RudpChannelConfig) super.config();
    }

    @Override
    public VirtualUnsafe unsafe() {
        return (VirtualUnsafe) super.unsafe();
    }

    public static interface VirtualUnsafe<T> extends Unsafe<T> {

        void messageReceived(Object message);
    }

    private final static class Entry implements Comparable<Entry> {

        private int sequence;
        private Object message;
        private ChannelPromise channelPromise;

        private Entry(int sequence, Object message, ChannelPromise channelPromise) {
            this.sequence = sequence;
            this.message = message;
            this.channelPromise = channelPromise;
        }

        @Override
        public int compareTo(Entry o) {
            if (o == this) {
                return 0;
            }
            int s1 = this.sequence;
            int s2 = o.sequence;
            int max = Integer.MAX_VALUE;
            if ((s1 > s2) && (s1 - s2 <= max / 2) || (s2 > s1) && (s2 - s1 > max / 2)) {
                return s1 > s2 ? 1 : -1;
            } else {
                return s1 < s2 ? 1 : -1;
            }
        }
    }

    private class RudpVirtualChannelUnsafe extends AbstractUnsafe<Void> implements VirtualUnsafe<Void> {

        private final Runnable TIMEOUT_TASK = new Runnable() {
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

        private final Queue<Entry> inboundBuffer = new PriorityQueue<>();
        private final Queue<Entry> outboundBuffer = new PriorityQueue<>();
        private ScheduledFuture<?> timeoutFuture;

        @Override
        public void messageReceived(Object message) {
            Entry entry = createEntry((byte[]) message);
            inboundBuffer.add(entry);

            if (isRegistered() && config().isAutoRead()) {
                readRequested();
                scheduleTimeoutTask();
            }
        }

        private Entry createEntry(byte[] message) {
            ByteBuffer buffer = byteBufferPool().acquire(message.length, false).put(message);
            try {
                buffer.flip();
                return createEntry0(buffer);
            } finally {
                byteBufferPool().release(buffer);
            }
        }

        private Entry createEntry0(ByteBuffer packet) {
            int seqNumber = packet.getInt();
            int ackNumber = packet.getInt();
            byte[] data = Buffers.getBytes(packet);
            if (remoteSeq.get() < seqNumber) {
                remoteSeq.set(seqNumber);
            }
            return new Entry(seqNumber, data, null);
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
        protected void writeRequested() {
            if (eventLoop().inExecutorThread()) {
                WRITE_TASK.run();
            } else {
                invokeLater(WRITE_TASK);
            }
        }

        @Override
        protected boolean isActive() {
            return isOpen();
        }

        @Override
        protected boolean isOpen() {
            return STATE_UPDATER.get(RudpVirtualChannel.this) == ST_ACTIVE;
        }

        @Override
        protected void afterRegister() {
            STATE_UPDATER.set(RudpVirtualChannel.this, ST_ACTIVE);
            scheduleTimeoutTask();
        }

        @Override
        protected void afterUnregister() {
            cancelTimeoutTask();
        }

        private void scheduleTimeoutTask() {
            if (timeoutFuture != null) {
                timeoutFuture.cancel();
            }
            timeoutFuture = eventLoop().schedule(TIMEOUT_TASK, config().getDisconnectTimeoutMillis(), TimeUnit.MILLISECONDS);
        }

        private void cancelTimeoutTask() {
            if (timeoutFuture != null) {
                timeoutFuture.cancel();
                timeoutFuture = null;
            }
        }

        private void read() {
            if (inboundBuffer.isEmpty()) {
                return;
            }

            Throwable error = null;
            try {
                for (int i = 0; i < config().getReadSpinCount(); i++) {
                    Entry message = inboundBuffer.peek();
                    if (message == null) {
                        break;
                    }

                    inboundBuffer.poll();
                    pipeline().fireMessageReceived(message.message);
                }
            } catch (Throwable e) {
                error = e;
            }

            if (error != null) {
                pipeline().fireExceptionCaught(error);
                close(voidPromise());
            }
        }

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            byte[] snd = writeSequenceHeader((byte[]) message);
            super.write(new Datagram(snd, remoteAddress), channelPromise);
        }

        private byte[] writeSequenceHeader(byte[] message) {
            ByteBufferPool alloc = byteBufferPool();
            DynamicByteBuffer dynamicBuffer = alloc.acquireDynamic(8, false);

            try {
                dynamicBuffer.putInt(localSeq.getAndIncrement());
                dynamicBuffer.putInt(remoteSeq.get());
                dynamicBuffer.put(message);
                dynamicBuffer.flip();
                return Buffers.getBytes(dynamicBuffer.asByteBuffer());
            } finally {
                alloc.release(dynamicBuffer);
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
        public void connect(InetSocketAddress address, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void closeForcibly() {
            STATE_UPDATER.set(RudpVirtualChannel.this, ST_INACTIVE);
        }

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return parent().getLocalAddress();
        }

        @Override
        public SocketAddress getRemoteAddress() throws Exception {
            return remoteAddress;
        }
    }
}
