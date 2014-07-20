package io.gwynt.core.rudp;

import io.gwynt.core.AbstractVirtualChannel;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Datagram;
import io.gwynt.core.ServerChannel;
import io.gwynt.core.concurrent.ScheduledFuture;
import io.gwynt.core.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class NioServerDatagramChannel extends NioDatagramChannel implements ServerChannel {

    @Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioServerDatagramChannelUnsafe();
    }

    @Override
    protected ChannelConfig newConfig() {
        return new RudpChannelConfig(this);
    }

    private final static class DatagramVirtualChannel extends AbstractVirtualChannel {

        private SocketAddress remoteAddress;

        private DatagramVirtualChannel(Channel parent) {
            super(parent);
        }

        @Override
        protected AbstractVirtualUnsafe newUnsafe() {
            return new DatagramVirtualChannelUnsafe();
        }

        private class DatagramVirtualChannelUnsafe extends AbstractVirtualUnsafe {

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

    protected class NioServerDatagramChannelUnsafe extends NioDatagramChannelUnsafe {

        private final ChannelFutureListener childListener = new ChannelFutureListener() {
            @Override
            public void onComplete(ChannelFuture future) {
                children.remove(future.channel().getRemoteAddress());
            }
        };

        private final Map<SocketAddress, AbstractVirtualChannel> children = new ConcurrentHashMap<>();

        @Override
        protected int doReadMessages(List<Object> messages) throws Exception {
            int accepted = 0;
            List<Object> dgrams = new ArrayList<>();

            int read = super.doReadMessages(dgrams);
            for (int i = 0; i < read; i++) {
                Datagram dgram = (Datagram) dgrams.get(i);
                SocketAddress address = dgram.sender();
                if (!children.containsKey(address)) {
                    DatagramVirtualChannel ch = new DatagramVirtualChannel(NioServerDatagramChannel.this);
                    ch.closeFuture().addListener(childListener);
                    ch.remoteAddress = address;
                    messages.add(ch);
                    children.put(address, ch);
                    accepted++;
                }
                AbstractVirtualChannel ch = children.get(address);
                ch.unsafe().messageReceived(dgram.content());
            }

            return accepted;
        }

        @Override
        public void connect(InetSocketAddress address, ChannelPromise channelPromise) {
            safeSetFailure(channelPromise, new UnsupportedOperationException());
        }
    }
}
