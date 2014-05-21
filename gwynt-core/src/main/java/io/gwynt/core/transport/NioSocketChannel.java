package io.gwynt.core.transport;

import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.exception.ChannelException;
import io.gwynt.core.exception.EofException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

public class NioSocketChannel extends AbstractNioChannel {

    @SuppressWarnings("unused")
    public NioSocketChannel(Endpoint endpoint) throws IOException {
        this(null, endpoint, SocketChannel.open());
    }

    public NioSocketChannel(AbstractNioChannel parent, Endpoint endpoint, SocketChannel ch) {
        super(parent, endpoint, ch);
    }

    @Override
    protected Unsafe newUnsafe() {
        return new NioSocketChannelUnsafe();
    }

    private class NioSocketChannelUnsafe extends AbstractNioUnsafe<SocketChannel> {

        private final ChannelPromise connectPromise = newChannelPromise();

        @Override
        protected void closeRequested() {
            interestOps(SelectionKey.OP_WRITE);
        }

        @Override
        public void connect(InetSocketAddress address, ChannelPromise channelPromise) {
            connectPromise.chainPromise(channelPromise);
            try {
                boolean connected = javaChannel().connect(address);
                if (!connected) {
                    interestOps(SelectionKey.OP_CONNECT);
                } else {
                    connectPromise.complete();
                }
            } catch (IOException e) {
                connectPromise.complete(e);
            }
        }

        @Override
        protected void doAfterRegister() {
            super.doAfterRegister();
            if (isActive()) {
                pipeline().fireOpen();
            }
        }

        @Override
        protected void doReadMessages(List<Object> messages) {
            // TODO make channel config
            ByteBuffer buffer = endpoint().getByteBufferPool().acquire(4096, true);
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
                    endpoint().getByteBufferPool().release(buffer);
                    return;
                }
            } while (buffer.hasRemaining() && bytesWritten > 0);

            endpoint().getByteBufferPool().release(buffer);

            if (bytesWritten == -1) {
                throw new EofException();
            }
        }

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            ByteBuffer buffer;
            if (message instanceof byte[]) {
                byte[] bytes = (byte[]) message;
                buffer = endpoint().getByteBufferPool().acquire(bytes.length, true);
                buffer.put(bytes);
                buffer.flip();
            } else if (message instanceof ByteBuffer) {
                ByteBuffer byteBuffer = (ByteBuffer) message;
                byteBuffer.flip();
                buffer = byteBuffer;
            } else {
                throw new IllegalArgumentException("Wrong message type");
            }
            super.write(buffer, channelPromise);
        }

        @Override
        protected boolean doWriteMessage(Object message) {
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
            if (!src.hasRemaining()) {
                endpoint().getByteBufferPool().release(src);
                return true;
            }
            return false;
        }

        @Override
        public void doConnect() throws IOException {
            boolean wasActive = isActive();
            if (javaChannel().finishConnect()) {
                connectPromise.complete();
                if (!wasActive && isActive()) {
                    interestOps(SelectionKey.OP_READ);
                    pipeline().fireOpen();
                }
            } else {
                doCloseChannel();
                throw new ChannelException("Connection failed");
            }
        }

        @Override
        protected boolean isActive() {
            return javaChannel().isOpen() && javaChannel().isConnected();
        }

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return javaChannel().getLocalAddress();
        }

        @Override
        public SocketAddress getRemoteAddress() throws Exception {
            return javaChannel().getRemoteAddress();
        }
    }
}
