package io.gwynt.core.oio;

import io.gwynt.core.AbstractVirtualChannel;
import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Datagram;
import io.gwynt.core.DatagramVirtualChannel;
import io.gwynt.core.ServerChannel;
import io.gwynt.core.rudp.RudpChannelConfig;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OioServerDatagramChannel extends OioDatagramChannel implements ServerChannel {

    @Override
    protected AbstractOioUnsafe newUnsafe() {
        return new OioServerDatagramChannelUnsafe();
    }

    @Override
    protected ChannelConfig newConfig() {
        return new RudpChannelConfig(this);
    }

    protected class OioServerDatagramChannelUnsafe extends OioDatagramChannelUnsafe {

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
                    DatagramVirtualChannel ch = new DatagramVirtualChannel(OioServerDatagramChannel.this, address);
                    ch.closeFuture().addListener(childListener);
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
