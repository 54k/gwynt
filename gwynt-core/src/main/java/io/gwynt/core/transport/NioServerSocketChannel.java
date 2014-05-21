package io.gwynt.core.transport;

import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

public class NioServerSocketChannel extends AbstractNioChannel {

    public NioServerSocketChannel() throws IOException {
        super(ServerSocketChannel.open());
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
            scheduler().schedule(CLOSE_TASK);
        }

        @Override
        public void bind(InetSocketAddress address, ChannelPromise channelPromise) {
            try {
                javaChannel().bind(address);
                interestOps(SelectionKey.OP_ACCEPT);
                channelPromise.complete();
            } catch (IOException e) {
                channelPromise.complete(e);
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
        protected void doReadMessages(List<Object> messages) {
            SocketChannel ch;
            try {
                ch = javaChannel().accept();
                ch.configureBlocking(false);
            } catch (IOException e) {
                exceptionCaught(e);
                return;
            }
            NioSocketChannel channel = new NioSocketChannel(NioServerSocketChannel.this, ch);
            messages.add(channel);
        }

        @Override
        protected boolean doWriteMessage(Object message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return javaChannel().getLocalAddress();
        }
    }
}
