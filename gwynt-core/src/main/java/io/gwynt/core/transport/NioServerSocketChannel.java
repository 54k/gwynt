package io.gwynt.core.transport;

import io.gwynt.core.Endpoint;

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
        protected void doAccept0(List<AbstractNioChannel> channels) {
            try {
                SocketChannel ch = javaChannel().accept();
                ch.configureBlocking(false);
                NioSocketChannel channel = new NioSocketChannel(NioServerSocketChannel.this, endpoint, ch);
                channels.add(channel);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void bind(InetSocketAddress address) {
            try {
                javaChannel().bind(address);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void doRegister0(Dispatcher dispatcher) {
            dispatcher.modifyRegistration(NioServerSocketChannel.this, SelectionKey.OP_ACCEPT);
        }

        @Override
        protected void doUnregister0(Dispatcher dispatcher) {
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
