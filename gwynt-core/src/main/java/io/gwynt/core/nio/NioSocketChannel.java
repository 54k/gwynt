package io.gwynt.core.nio;

import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.RecvByteBufferAllocator;
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
        private RecvByteBufferAllocator.Handle allocHandle;

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
                    connectPromise.setSuccess();
                }
            } catch (IOException e) {
                connectPromise.setFailure(e);
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
            if (allocHandle == null) {
                allocHandle = config().getRecvByteBufferAllocator().newHandle();
            }
            ByteBuffer buffer = allocHandle.allocate(config().getByteBufferPool());
            Throwable error = null;
            int bytesRead = 0;
            int messagesRead = 0;

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
            }

            if (error != null) {
                exceptionCaught(error);
            }

            if (bytesRead == -1) {
                doClose();
            }

            if (bytesRead > 0) {
                allocHandle.record(bytesRead);
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
            }

            if (bytesWritten == -1) {
                doClose();
            }

            return !src.hasRemaining();
        }

        @Override
        public void doConnect() {
            assert eventLoop().inExecutorThread();

            boolean wasActive = isActive();
            try {
                if (javaChannel().finishConnect()) {
                    connectPromise.setSuccess();
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
            } catch (IOException e) {
                connectPromise.setFailure(e);
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
