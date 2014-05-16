package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.exception.ChannelException;
import io.gwynt.core.util.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

public class NioServerSocketChannel extends AbstractNioChannel {

    public NioServerSocketChannel(Endpoint endpoint) {
        super(endpoint);
        try {
            unsafe = new NioServerSocketChannelUnsafe(ServerSocketChannel.open());
        } catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public ChannelFuture read() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelFuture write(Object message) {
        throw new UnsupportedOperationException();
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

    private class NioServerSocketChannelUnsafe extends AbstractNioUnsafe<ServerSocketChannel> {

        private NioServerSocketChannelUnsafe(ServerSocketChannel ch) {
            super(ch);
        }

        @Override
        protected void doAcceptImpl(List<Pair<Channel, ChannelPromise>> channels) {
            SocketChannel ch;
            try {
                ch = javaChannel().accept();
                ch.configureBlocking(false);
            } catch (IOException e) {
                exceptionCaught(e);
                return;
            }

            NioSocketChannel channel = new NioSocketChannel(NioServerSocketChannel.this, endpoint(), ch);
            ChannelPromise channelPromise = channel.newChannelPromise();
            channelPromise.addListener(new ChannelFutureListener() {
                @Override
                public void onComplete(ChannelFuture channelFuture) {
                    channelFuture.channel().unsafe().read(VOID_PROMISE);
                }

                @Override
                public void onError(ChannelFuture channelFuture, Throwable e) {
                }
            });
            channels.add(new Pair<Channel, ChannelPromise>(channel, channelPromise));
        }

        @Override
        public ChannelFuture bind(InetSocketAddress address, ChannelPromise channelPromise) {
            try {
                javaChannel().bind(address);
                dispatcher().modifyRegistration(NioServerSocketChannel.this, SelectionKey.OP_ACCEPT);
                return channelPromise.complete();
            } catch (IOException e) {
                return channelPromise.complete(e);
            }
        }

        @Override
        protected void doAfterUnregister() {
            // NO OP
        }

        @Override
        protected void doAfterRegister() {
            // NO OP
        }

        @Override
        public ChannelFuture read(ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture write(Object message, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doReadImpl(List<Object> messages) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean doWriteImpl(Object message) {
            throw new UnsupportedOperationException();
        }
    }
}
