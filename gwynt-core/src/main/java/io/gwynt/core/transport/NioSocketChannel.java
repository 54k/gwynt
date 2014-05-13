package io.gwynt.core.transport;

import io.gwynt.core.Endpoint;
import io.gwynt.core.exception.EofException;
import io.gwynt.core.util.ByteBufferAllocator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;

public class NioSocketChannel extends AbstractNioChannel {

    public NioSocketChannel(Endpoint endpoint) {
        super(endpoint);
        try {
            SocketChannel ch = SelectorProvider.provider().openSocketChannel();
            ch.configureBlocking(false);
            unsafe = new NioSocketUnsafe(ch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public NioSocketChannel(AbstractNioChannel parent, Endpoint endpoint, SocketChannel ch) {
        super(parent, endpoint);
        try {
            ch.configureBlocking(false);
            unsafe = new NioSocketUnsafe(ch);
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

    private class NioSocketUnsafe extends AbstractUnsafe<SocketChannel> {

        private NioSocketUnsafe(SocketChannel ch) {
            super(ch);
        }

        @Override
        public void connect(InetSocketAddress address) {
            try {
                boolean connected = javaChannel().connect(address);
                if (!connected) {
                    dispatcher().modifyRegistration(NioSocketChannel.this, SelectionKey.OP_CONNECT);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void doRegister0(Dispatcher dispatcher) {
            dispatcher.modifyRegistration(NioSocketChannel.this, SelectionKey.OP_READ);
            if (isActive()) {
                pipeline().fireOpen();
            }
        }

        @Override
        protected void doUnregister0(Dispatcher dispatcher) {
            if (!isActive()) {
                pipeline().fireClose();
            }
        }

        @Override
        protected void doAccept0(List<AbstractNioChannel> channels) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doRead0(List<Object> messages) {
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
                    throw new RuntimeException(e);
                }
            } while (buffer.hasRemaining() && bytesWritten > 0);

            ByteBufferAllocator.release(buffer);

            if (bytesWritten == -1) {
                throw new EofException();
            }
        }

        @Override
        public void write(Object message) {
            super.write(ByteBuffer.wrap((byte[]) message));
        }

        @Override
        protected boolean doWrite0(Object message) {
            int bytesWritten;
            ByteBuffer src = (ByteBuffer) message;
            do {
                try {
                    bytesWritten = javaChannel().write(src);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } while (src.hasRemaining() && bytesWritten > 0);

            if (bytesWritten == -1) {
                throw new EofException();
            }

            return !src.hasRemaining();
        }

        @Override
        public void doConnect() throws IOException {
            javaChannel().finishConnect();
            pipeline().fireOpen();
        }
    }
}