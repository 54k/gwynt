package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.exception.EofException;
import io.gwynt.core.util.ByteBufferAllocator;
import io.gwynt.core.util.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;

public class NioSocketChannel extends AbstractNioChannel {

    @SuppressWarnings("unused")
    public NioSocketChannel(Endpoint endpoint) {
        super(endpoint);
        try {
            SocketChannel ch = SelectorProvider.provider().openSocketChannel();
            ch.configureBlocking(false);
            unsafe = new NioSocketNioUnsafe(ch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public NioSocketChannel(AbstractNioChannel parent, Endpoint endpoint, SocketChannel ch) {
        super(parent, endpoint);
        try {
            ch.configureBlocking(false);
            unsafe = new NioSocketNioUnsafe(ch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SocketAddress getLocalAddress() {
        try {
            return ((SocketChannel) unsafe().javaChannel()).getLocalAddress();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public SocketAddress getRemoteAddress() {
        try {
            return ((SocketChannel) unsafe().javaChannel()).getRemoteAddress();
        } catch (IOException e) {
            return null;
        }
    }

    private class NioSocketNioUnsafe extends AbstractNioUnsafe<SocketChannel> {

        private final ChannelPromise connectFuture = newChannelPromise();

        private NioSocketNioUnsafe(SocketChannel ch) {
            super(ch);
        }

        @Override
        protected void closeImpl() {
            if (isRegistered()) {
                dispatcher().modifyRegistration(NioSocketChannel.this, SelectionKey.OP_WRITE);
            }
        }

        @Override
        public void connect(InetSocketAddress address, ChannelPromise channelPromise) {
            connectFuture.chainPromise(channelPromise);
            try {
                boolean connected = javaChannel().connect(address);
                if (!connected) {
                    dispatcher().modifyRegistration(NioSocketChannel.this, SelectionKey.OP_CONNECT);
                } else {
                    connectFuture.complete();
                }
            } catch (IOException e) {
                connectFuture.complete(e);
            }
        }

        @Override
        protected void doAfterRegister() {
            pipeline().fireRegistered();
            if (isActive()) {
                pipeline().fireOpen();
            }
        }

        @Override
        protected void doAfterUnregister() {
            pipeline().fireUnregistered();
        }

        @Override
        protected void doAcceptImpl(List<Pair<Channel, ChannelPromise>> channels) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doReadImpl(List<Object> messages) {
            ByteBuffer buffer = ByteBufferAllocator.allocate(4096);
            int bytesWritten;

            do {
                try {
                    bytesWritten = javaChannel().read(buffer);
                    if (bytesWritten > 0) {
                        buffer.flip();
                        byte[] message = new byte[buffer.limit()];
                        buffer.get(message);
                        messages.add(message);
                    }
                } catch (IOException e) {
                    exceptionCaught(e);
                    return;
                }
            } while (buffer.hasRemaining() && bytesWritten > 0);

            ByteBufferAllocator.release(buffer);

            if (bytesWritten == -1) {
                throw new EofException();
            }
        }

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            super.write(ByteBuffer.wrap((byte[]) message), channelPromise);
        }

        @Override
        protected boolean doWriteImpl(Object message) {
            int bytesWritten;
            ByteBuffer src = (ByteBuffer) message;
            do {
                try {
                    bytesWritten = javaChannel().write(src);
                } catch (IOException e) {
                    throw new EofException();
                }
            } while (src.hasRemaining() && bytesWritten > 0);

            if (bytesWritten == -1) {
                throw new EofException();
            }

            return !src.hasRemaining();
        }

        @Override
        public void doConnect() throws IOException {
            boolean wasActive = isActive();
            if (javaChannel().finishConnect()) {
                connectFuture.complete();
                if (!wasActive && isActive()) {
                    dispatcher().modifyRegistration(NioSocketChannel.this, SelectionKey.OP_READ);
                    pipeline().fireOpen();
                }
            } else {
                doCloseImpl();
                throw new RuntimeException("Connection failed");
            }
        }

        @Override
        protected boolean isActive() {
            return javaChannel().isOpen() && javaChannel().isConnected();
        }

        @Override
        protected void doCloseImpl() {
            super.doCloseImpl();
            closePromise().complete();
            pipeline().fireClose();
        }
    }
}
