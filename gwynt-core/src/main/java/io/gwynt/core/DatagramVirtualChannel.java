package io.gwynt.core;

import io.gwynt.core.concurrent.ScheduledFuture;
import io.gwynt.core.rudp.RudpChannelConfig;

import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class DatagramVirtualChannel extends AbstractVirtualChannel {

    private SocketAddress remoteAddress;

    public DatagramVirtualChannel(Channel parent, SocketAddress remoteAddress) {
        super(parent);
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        this.remoteAddress = remoteAddress;
    }

    @Override
    protected AbstractVirtualUnsafe newUnsafe() {
        return new DatagramVirtualChannelUnsafe();
    }

    protected class DatagramVirtualChannelUnsafe extends AbstractVirtualUnsafe {

        private final Runnable timeoutTask = new Runnable() {
            @Override
            public void run() {
                close(voidPromise());
            }
        };
        private final Queue<byte[]> readBuf = new ConcurrentLinkedQueue<>();
        private ScheduledFuture<?> timeoutFuture;

        @Override
        protected void readReceivedMessages() {
            while (readBuf.peek() != null) {
                pipeline().fireMessageReceived(readBuf.poll());
            }
        }

        @Override
        public void messageReceived(Object message) {
            readBuf.add((byte[]) message);
            if (isActive()) {
                readRequested();
                scheduleTimeoutTask();
            }
        }

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            super.write(new Datagram((byte[]) message, remoteAddress, parent().getLocalAddress()), channelPromise);
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
            timeoutFuture = eventLoop().schedule(timeoutTask, ((RudpChannelConfig) config()).getDisconnectTimeoutMillis(), TimeUnit.MILLISECONDS);
        }

        private void purgeTimeoutTask() {
            if (timeoutFuture != null) {
                timeoutFuture.cancel();
                timeoutFuture = null;
            }
        }

        @Override
        public SocketAddress getRemoteAddress() throws Exception {
            return remoteAddress;
        }
    }
}
