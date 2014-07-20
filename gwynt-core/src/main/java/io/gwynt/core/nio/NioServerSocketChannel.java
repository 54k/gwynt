package io.gwynt.core.nio;

import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelException;
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

    @SuppressWarnings("unused")
    public NioServerSocketChannel() {
        this(newSocket());
    }

    public NioServerSocketChannel(ServerSocketChannel ch) {
        super(ch);
        readOp = SelectionKey.OP_ACCEPT;
    }

    private static ServerSocketChannel newSocket() {
        try {
            return ServerSocketChannel.open();
        } catch (IOException e) {
            throw new ChannelException(e);
        }
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
    protected AbstractNioUnsafe newUnsafe() {
        return new NioServerSocketChannelUnsafe();
    }

    @Override
    protected ChannelConfig newConfig() {
        return new NioServerSocketChannelConfig(this);
    }

    @Override
    public NioServerSocketChannelConfig config() {
        return (NioServerSocketChannelConfig) super.config();
    }

    @Override
    public ServerSocketChannel javaChannel() {
        return (ServerSocketChannel) super.javaChannel();
    }

    protected class NioServerSocketChannelUnsafe extends AbstractNioUnsafe {

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
        protected void writeRequested() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int doReadMessages(List<Object> messages) throws Exception {
            try {
                SocketChannel ch = javaChannel().accept();
                if (ch != null) {
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
