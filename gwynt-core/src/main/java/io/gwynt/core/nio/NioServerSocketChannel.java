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

    private class NioServerSocketChannelUnsafe extends AbstractNioUnsafe<ServerSocketChannel> {

        private final Runnable CLOSE_TASK = new Runnable() {
            @Override
            public void run() {
                doClose();
            }
        };

        @Override
        protected void closeRequested() {
            eventLoop().execute(CLOSE_TASK);
        }

        @Override
        public void bind(InetSocketAddress address, ChannelPromise channelPromise) {
            try {
                javaChannel().bind(address);
                interestOps(SelectionKey.OP_ACCEPT);
                safeSetSuccess(channelPromise);
            } catch (IOException e) {
                safeSetFailure(channelPromise, e);
            }
        }

        @Override
        public void read(ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int doReadMessages(List<Object> messages) {
            SocketChannel ch;
            int accepted = 0;
            try {
                ch = javaChannel().accept();
                if (ch == null) {
                    return 0;
                }
                ch.configureBlocking(false);
                accepted++;
            } catch (IOException e) {
                exceptionCaught(e);
                return 0;
            }

            NioSocketChannel channel = new NioSocketChannel(NioServerSocketChannel.this, ch);
            messages.add(channel);

            if (!config().isAutoRead()) {
                interestOps(interestOps() & ~SelectionKey.OP_ACCEPT);
            }

            return accepted;
        }

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return javaChannel().getLocalAddress();
        }
    }
}
