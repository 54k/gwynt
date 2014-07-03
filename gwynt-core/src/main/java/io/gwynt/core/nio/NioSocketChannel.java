package io.gwynt.core.nio;

import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelException;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.RecvByteBufferAllocator;
import io.gwynt.core.concurrent.ScheduledFuture;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @Override
    public NioSocketChannelConfig config() {
        return (NioSocketChannelConfig) super.config();
    }

    private class NioSocketChannelUnsafe extends AbstractNioUnsafe<SocketChannel> {

        private ScheduledFuture<?> connectTimeout;
        private ChannelPromise connectPromise;

        @Override
        public void connect(final InetSocketAddress address, ChannelPromise channelPromise) {
            connectPromise = channelPromise;
            try {
                boolean connected = javaChannel().connect(address);
                if (!connected) {
                    interestOps(SelectionKey.OP_CONNECT);

                    long connectTimeoutMillis = config().getConnectTimeoutMillis();
                    if (connectTimeoutMillis > 0) {
                        connectTimeout = eventLoop().schedule(new Runnable() {
                            @Override
                            public void run() {
                                ConnectException cause = new ConnectException("connect timeout: " + address);
                                if (connectPromise != null && connectPromise.tryFailure(cause)) {
                                    doClose();
                                }
                            }
                        }, config().getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
                    }

                    channelPromise.addListener(new ChannelFutureListener() {
                        @Override
                        public void onComplete(ChannelFuture future) {
                            if (future.isCancelled()) {
                                if (connectTimeout != null) {
                                    connectTimeout.cancel();
                                }
                                connectPromise = null;
                                doClose();
                            }
                        }
                    });
                } else {
                    connectPromise.setSuccess();
                }
            } catch (IOException e) {
                connectPromise.setFailure(e);
            }
        }

        @Override
        protected void doDisconnect() {
            connectPromise = null;
            doClose();
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
        protected void flush0(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
            NioSocketChannelOutboundBuffer outboundBuffer = (NioSocketChannelOutboundBuffer) channelOutboundBuffer;
            long remainingBytes = outboundBuffer.remaining();
            ByteBuffer[] buffers = outboundBuffer.byteBuffers();

            for (int i = 0; i < config().getWriteSpinCount(); i++) {
                long bytesWritten = javaChannel().write(buffers);

                if (bytesWritten == -1) {
                    doClose();
                    return;
                }

                remainingBytes -= bytesWritten;
                if (remainingBytes == 0) {
                    break;
                }
            }

            for (ByteBuffer buffer : buffers) {
                if (buffer.hasRemaining()) {
                    writeRequested();
                    break;
                } else {
                    outboundBuffer.remove();
                }
            }
        }

        @Override
        public void connect() {
            boolean wasActive = isActive();
            try {
                if (javaChannel().finishConnect()) {
                    boolean connectSuccess = connectPromise.trySuccess();

                    if (!wasActive && isActive()) {
                        if (config().isAutoRead()) {
                            readRequested();
                        }
                        pipeline().fireOpen();
                    }

                    if (!connectSuccess) {
                        doClose();
                    }
                } else {
                    closeJavaChannel();
                    connectPromise.tryFailure(new ChannelException("Connection failed"));
                }
            } catch (IOException e) {
                connectPromise.tryFailure(e);
                doClose();
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
