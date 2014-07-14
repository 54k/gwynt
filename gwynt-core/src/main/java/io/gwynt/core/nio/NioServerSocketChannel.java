package io.gwynt.core.nio;

import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.ServerChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

public class NioServerSocketChannel extends AbstractNioChannel implements ServerChannel {

    public NioServerSocketChannel() throws IOException {
        super(ServerSocketChannel.open());
        readOp = SelectionKey.OP_ACCEPT;
    }

    @Override
    public ChannelFuture read() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelFuture write(Object message, ChannelPromise channelPromise) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelFuture write(Object message) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Unsafe newUnsafe() {
        return new NioServerSocketChannelUnsafe();
    }

    protected class NioServerSocketChannelUnsafe extends AbstractNioUnsafe<ServerSocketChannel> {

        @Override
        protected void doBind(InetSocketAddress address, ChannelPromise channelPromise) throws Exception {
            javaChannel().bind(address);
            if (config().isAutoRead()) {
                readRequested();
            }
        }

        @Override
        protected boolean doConnect(InetSocketAddress address, ChannelPromise channelPromise) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean doFinishConnect() throws Exception {
            throw new Error();
        }

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            safeSetFailure(channelPromise, new UnsupportedOperationException());
        }

        @Override
        protected int doReadMessages(List<Object> messages) throws Exception {
            SocketChannel ch = javaChannel().accept();
            try {
                if (ch != null) {
                    ch.configureBlocking(false);
                    NioSocketChannel channel = new NioSocketChannel(NioServerSocketChannel.this, ch);
                    messages.add(channel);
                    return 1;
                }
            } catch (IOException e) {
                exceptionCaught(e);
                ch.close();
            }
            return 0;
        }

        @Override
        public boolean isActive() {
            return isOpen() && javaChannel().socket().isBound();
        }

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return javaChannel().getLocalAddress();
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }
    }
}
