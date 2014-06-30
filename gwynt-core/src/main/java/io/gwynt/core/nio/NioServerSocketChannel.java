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

    private class NioServerSocketChannelUnsafe extends AbstractNioUnsafe<ServerSocketChannel> {

        private final Runnable READ_TASK = new Runnable() {
            @Override
            public void run() {
                interestOps(interestOps() | SelectionKey.OP_ACCEPT);
            }
        };
        private final Runnable CLOSE_TASK = new Runnable() {
            @Override
            public void run() {
                doClose();
            }
        };

        @Override
        protected void beforeClose() {
            invokeLater(CLOSE_TASK);
        }

        @Override
        public void bind(InetSocketAddress address, ChannelPromise channelPromise) {
            try {
                javaChannel().bind(address);
                safeSetSuccess(channelPromise);
                pipeline().fireOpen();
                if (config().isAutoRead()) {
                    interestOps(SelectionKey.OP_ACCEPT);
                }
            } catch (IOException e) {
                safeSetFailure(channelPromise, e);
            }
        }

        @Override
        protected void readRequested() {
            if (eventLoop().inExecutorThread()) {
                interestOps(SelectionKey.OP_ACCEPT);
            } else {
                invokeLater(READ_TASK);
            }
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
            }
            return 0;
        }

        @Override
        protected boolean isActive() {
            return isOpen() && javaChannel().socket().isBound();
        }

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return javaChannel().getLocalAddress();
        }
    }
}
