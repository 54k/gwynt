package io.gwynt.core.nio;

import io.gwynt.core.AbstractVirtualChannel;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Datagram;
import io.gwynt.core.ServerChannel;
import io.gwynt.core.concurrent.ScheduledFuture;
import io.gwynt.core.rudp.RudpChannelConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class NioServerDatagramChannel extends NioDatagramChannel implements ServerChannel {

    public NioServerDatagramChannel() throws IOException {
    }

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

        private class DatagramVirtualChannelUnsafe extends AbstractVirtualUnsafe<Void> {

            private final Runnable TIMEOUT_TASK = new Runnable() {
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
                super.write(new Datagram((byte[]) message, remoteAddress), channelPromise);
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
                timeoutFuture = eventLoop().schedule(TIMEOUT_TASK, ((RudpChannelConfig) config()).getDisconnectTimeoutMillis(), TimeUnit.MILLISECONDS);
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

        private final ChannelFutureListener CLOSE_LISTENER = new ChannelFutureListener() {
            @Override
            public void onComplete(ChannelFuture future) {
                children.remove(future.channel().getRemoteAddress());
            }
        };

        private final Map<SocketAddress, AbstractVirtualChannel> children = new HashMap<>();

        @Override
        protected int doReadMessages(List<Object> messages) throws Exception {
            int accepted = 0;
            List<Object> dgrams = new ArrayList<>();

            int read = super.doReadMessages(dgrams);
            for (int i = 0; i < read; i++) {
                Datagram dgram = (Datagram) dgrams.get(i);
                SocketAddress address = dgram.recipient();
                if (!children.containsKey(address)) {
                    DatagramVirtualChannel ch = new DatagramVirtualChannel(NioServerDatagramChannel.this);
                    ch.closeFuture().addListener(CLOSE_LISTENER);
                    ch.remoteAddress = address;
                    messages.add(ch);
                    children.put(dgram.recipient(), ch);
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
