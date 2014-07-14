package io.gwynt.core.nio;

import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.buffer.RecvByteBufferAllocator;

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
        return new NioSocketChannelConfig(this, javaChannel());
    }

    @Override
    public NioSocketChannelConfig config() {
        return (NioSocketChannelConfig) super.config();
    }

    @Override
    protected SocketChannel javaChannel() {
        return (SocketChannel) super.javaChannel();
    }

    private class NioSocketChannelUnsafe extends AbstractNioUnsafe<SocketChannel> {

        @Override
        protected boolean doConnect(final InetSocketAddress address, ChannelPromise channelPromise) throws Exception {
            boolean connected = javaChannel().connect(address);
            if (!connected) {
                interestOps(SelectionKey.OP_CONNECT);
            }
            return connected;
        }

        @Override
        protected void doDisconnect(ChannelPromise channelPromise) throws Exception {
            closeForcibly();
        }

        @Override
        protected int doReadMessages(List<Object> messages) throws Exception {
            RecvByteBufferAllocator.Handle allocHandle = allocHandle();
            ByteBuffer buffer = allocHandle.allocate(config().getByteBufferPool());

            int bytesRead = 0;
            try {
                bytesRead = javaChannel().read(buffer);
                if (bytesRead > 0) {
                    buffer.flip();
                    byte[] message = new byte[buffer.limit()];
                    buffer.get(message);
                    messages.add(message);
                    allocHandle.record(bytesRead);
                    return 1;
                }
            } finally {
                config().getByteBufferPool().release(buffer);
            }
            return bytesRead;
        }

        @Override
        protected ChannelOutboundBuffer newChannelOutboundBuffer() {
            return new NioSocketChannelOutboundBuffer(NioSocketChannel.this);
        }

        @Override
        protected void doWrite(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
            if (channelOutboundBuffer.size() == 1) {
                super.doWrite(channelOutboundBuffer);
                return;
            }

            NioSocketChannelOutboundBuffer outboundBuffer = (NioSocketChannelOutboundBuffer) channelOutboundBuffer;
            long remainingBytes = outboundBuffer.remaining();
            ByteBuffer[] buffers = outboundBuffer.byteBuffers();

            for (int i = 0; i < config().getWriteSpinCount(); i++) {
                long bytesWritten = javaChannel().write(buffers);

                remainingBytes -= bytesWritten;
                if (remainingBytes == 0) {
                    break;
                }
            }

            for (ByteBuffer buffer : buffers) {
                if (buffer.hasRemaining()) {
                    break;
                }
                outboundBuffer.remove();
            }
        }

        @Override
        protected boolean doWriteMessage(Object message) throws Exception {
            ByteBuffer buffer = (ByteBuffer) message;
            javaChannel().write(buffer);
            return buffer.hasRemaining();
        }

        @Override
        public boolean doFinishConnect() throws Exception {
            return javaChannel().finishConnect();
        }

        @Override
        public boolean isActive() {
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
