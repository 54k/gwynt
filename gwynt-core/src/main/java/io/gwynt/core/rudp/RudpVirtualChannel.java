package io.gwynt.core.rudp;

import io.gwynt.core.AbstractVirtualChannel;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Datagram;
import io.gwynt.core.buffer.Buffers;
import io.gwynt.core.buffer.ByteBufferPool;
import io.gwynt.core.buffer.DynamicByteBuffer;
import io.gwynt.core.concurrent.ScheduledFuture;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RudpVirtualChannel extends AbstractVirtualChannel {

    SocketAddress remoteAddress;
    AtomicInteger localSeq = new AtomicInteger();
    AtomicInteger remoteSeq = new AtomicInteger();

    public RudpVirtualChannel(Channel parent) {
        super(parent);
    }

    @Override
    public NioRudpServerChannel parent() {
        return (NioRudpServerChannel) super.parent();
    }

    @Override
    protected AbstractVirtualUnsafe newUnsafe() {
        return new RudpVirtualChannelUnsafe();
    }

    @Override
    public RudpChannelConfig config() {
        return (RudpChannelConfig) super.config();
    }

    @Override
    public VirtualUnsafe unsafe() {
        return (VirtualUnsafe) super.unsafe();
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

    private class RudpVirtualChannelUnsafe extends AbstractVirtualUnsafe {

        private final Runnable TIMEOUT_TASK = new Runnable() {
            @Override
            public void run() {
                close(voidPromise());
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
        protected void afterRegister() {
            super.afterRegister();
            scheduleTimeoutTask();
        }

        @Override
        protected void afterUnregister() {
            super.afterUnregister();
            purgeTimeoutTask();
        }

        private void scheduleTimeoutTask() {
            if (timeoutFuture != null) {
                timeoutFuture.cancel();
            }
            timeoutFuture = eventLoop().schedule(TIMEOUT_TASK, config().getDisconnectTimeoutMillis(), TimeUnit.MILLISECONDS);
        }

        private void purgeTimeoutTask() {
            if (timeoutFuture != null) {
                timeoutFuture.cancel();
                timeoutFuture = null;
            }
        }

        @Override
        protected void readReceivedMessages() {
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
            super.write(new Datagram(snd, remoteAddress, parent().getLocalAddress()), channelPromise);
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
        public SocketAddress getRemoteAddress() throws Exception {
            return remoteAddress;
        }
    }
}
