package io.gwynt.core.nio;

import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.exception.ChannelException;

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

    @Override
    protected ChannelConfig newConfig() {
        return new NioSocketChannelConfig(this, (SocketChannel) javaChannel());
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
            int bytesRead = 0;
            int messagesRead = 0;

            do {
                try {
                    bytesRead = javaChannel().read(buffer);
                    if (bytesRead > 0) {
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
            } while (buffer.hasRemaining() && bytesRead > 0);

            if (error != null) {
                exceptionCaught(error);
                bytesRead = -1;
            }
            if (bytesRead == -1) {
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
            Throwable error = null;
            int bytesWritten = 0;
            ByteBuffer src = (ByteBuffer) message;
            try {
                bytesWritten = javaChannel().write(src);
            } catch (IOException e) {
                error = e;
            }

            if (error != null) {
                exceptionCaught(error);
                bytesWritten = -1;
            }

            if (bytesWritten == -1) {
                doClose();
            }

            return !src.hasRemaining();
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
