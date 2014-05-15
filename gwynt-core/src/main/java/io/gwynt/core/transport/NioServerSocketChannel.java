package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.util.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

public class NioServerSocketChannel extends AbstractNioChannel {

    @SuppressWarnings("unused")
    public NioServerSocketChannel(Endpoint endpoint) {
        super(endpoint);
        try {
            unsafe = new NioServerSocketUnsafe(ServerSocketChannel.open());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SocketAddress getLocalAddress() {
        try {
            return ((ServerSocketChannel) unsafe.javaChannel()).getLocalAddress();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    private class NioServerSocketUnsafe extends AbstractUnsafe<ServerSocketChannel> {

        private NioServerSocketUnsafe(ServerSocketChannel ch) {
            super(ch);
        }

        @Override
        protected void doAccept0(List<Pair<AbstractNioChannel, ChannelPromise>> channels) {
            try {
                SocketChannel ch = javaChannel().accept();
                ch.configureBlocking(false);
                NioSocketChannel channel = new NioSocketChannel(NioServerSocketChannel.this, endpoint, ch);
                ChannelPromise channelPromise = channel.newChannelPromise();
                channelPromise.addListener(new ChannelFutureListener<Channel>() {
                    @Override
                    public void onComplete(Channel channel) {
                        channel.unsafe().read();
                    }

                    @Override
                    public void onError(Channel channel, Throwable e) {
                    }
                });
                channels.add(new Pair<AbstractNioChannel, ChannelPromise>(channel, channelPromise));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ChannelFuture bind(InetSocketAddress address) {
            ChannelPromise channelPromise = newChannelPromise();
            try {
                javaChannel().bind(address);
                dispatcher().modifyRegistration(NioServerSocketChannel.this, SelectionKey.OP_ACCEPT, channelPromise);
                return channelPromise;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void doRegister0() {
        }

        @Override
        protected void doUnregister0() {
            // NO-OP
        }

        @Override
        protected void doRead0(List<Object> messages) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean doWrite0(Object message) {
            throw new UnsupportedOperationException();
        }
    }
}
