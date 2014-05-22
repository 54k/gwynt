package io.gwynt.core.transport;

import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.NioSocketChannelOutboundBuffer;
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
    public NioSocketChannel() throws IOException {
        this(null, SocketChannel.open());
    }

    public NioSocketChannel(AbstractNioChannel parent, SocketChannel ch) {
        super(parent, ch);
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
        protected void afterRegister() {
            super.afterRegister();
            if (isActive()) {
                pipeline().fireOpen();
            }
        }

        @Override
        protected int doReadMessages(List<Object> messages) {
            ByteBuffer buffer = config().getByteBufferPool().acquire(4096, true);
            Throwable error = null;
            int bytesWritten = 0;
            int messagesRead = 0;

            do {
                try {
                    bytesWritten = javaChannel().read(buffer);
                    if (bytesWritten > 0) {
                        buffer.flip();
                        byte[] message = new byte[buffer.limit()];
                        buffer.get(message);
                        messages.add(message);
                        messagesRead++;
                    }
                } catch (IOException e) {
                    error = e;
                    break;
                }
            } while (buffer.hasRemaining() && bytesWritten > 0);

            if (error != null) {
                exceptionCaught(error);
                bytesWritten = -1;
            }
            if (bytesWritten == -1) {
                doClose();
            }

            config().getByteBufferPool().release(buffer);
            return messagesRead;
        }

        @Override
        protected ChannelOutboundBuffer newChannelOutboundBuffer() {
            return new NioSocketChannelOutboundBuffer(NioSocketChannel.this);
        }

        @Override
        protected boolean doWriteMessage(Object message) {
            int bytesWritten;
            ByteBuffer src = (ByteBuffer) message;
            do {
                try {
                    bytesWritten = javaChannel().write(src);
                } catch (IOException e) {
                    exceptionCaught(e);
                    config().getByteBufferPool().release(src);
                    return false;
                }
            } while (src.hasRemaining() && bytesWritten > 0);

            if (bytesWritten == -1) {
                throw new EofException();
            }
            if (!src.hasRemaining()) {
                config().getByteBufferPool().release(src);
                return true;
            }
            return false;
        }

        @Override
        public void doConnect() throws IOException {
            assert scheduler().inSchedulerThread();

            boolean wasActive = isActive();
            if (javaChannel().finishConnect()) {
                connectPromise.complete();
                if (!wasActive && isActive()) {
                    if (config().isAutoRead()) {
                        interestOps(SelectionKey.OP_READ);
                    }
                    pipeline().fireOpen();
                }
            } else {
                closeForcibly();
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
