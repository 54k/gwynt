package io.gwynt.core.oio;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelException;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.buffer.RecvByteBufferAllocator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;

public class OioSocketChannel extends AbstractOioChannel {

    @SuppressWarnings("unused")
    public OioSocketChannel() {
        this(null, newSocket());
    }

    public OioSocketChannel(Channel parent, Socket ch) {
        super(parent, ch);
    }

    private static Socket newSocket() {
        return new Socket();
    }

    @Override
    protected AbstractOioUnsafe newUnsafe() {
        return new OioSocketChannelUnsafe();
    }

    @Override
    protected ChannelConfig newConfig() {
        return new OioSocketChannelConfig(this);
    }

    @Override
    public OioSocketChannelConfig config() {
        return (OioSocketChannelConfig) super.config();
    }

    @Override
    public Socket javaChannel() {
        return (Socket) super.javaChannel();
    }

    protected class OioSocketChannelUnsafe extends AbstractOioUnsafe {

        private InputStream inputStream;
        private OutputStream outputStream;

        @Override
        public boolean isActive() {
            return isOpen() && javaChannel().isConnected();
        }

        @Override
        public boolean isOpen() {
            return !javaChannel().isClosed();
        }

        private InputStream getInputStream() throws IOException {
            return inputStream;
        }

        private OutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        private void initStreams() throws IOException {
            inputStream = javaChannel().getInputStream();
            outputStream = javaChannel().getOutputStream();
        }

        @Override
        protected void afterRegister() {
            if (isActive()) {
                try {
                    initStreams();
                } catch (IOException e) {
                    throw new ChannelException(e);
                }
            }
        }

        @Override
        protected void doConnect(InetSocketAddress address, ChannelPromise channelPromise) throws Exception {
            try {
                javaChannel().connect(address, config().getConnectTimeoutMillis());
                initStreams();
            } catch (SocketTimeoutException e) {
                throw new ChannelException("Connection timeout: " + address);
            } catch (IOException e) {
                throw new ChannelException(e);
            }
        }

        @Override
        protected void doDisconnect(ChannelPromise channelPromise) throws Exception {
            closeForcibly();
        }

        @Override
        protected int doReadMessages(List<Object> messages) throws Exception {
            if (!isActive()) {
                return -1;
            }

            RecvByteBufferAllocator.Handle allocHandle = allocHandle();
            byte[] buffer = new byte[allocHandle.guess()];

            int bytesRead = 0;
            try {
                bytesRead = getInputStream().read(buffer);
                if (bytesRead > 0) {
                    byte[] message = new byte[bytesRead];
                    System.arraycopy(buffer, 0, message, 0, bytesRead);
                    messages.add(message);
                    allocHandle.record(bytesRead);
                    return 1;
                }
            } catch (SocketTimeoutException ignore) {
            }
            return bytesRead;
        }

        @Override
        protected void doWrite(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
            boolean done = false;
            Object message = channelOutboundBuffer.current();
            if (message != null) {
                try {
                    getOutputStream().write((byte[]) message);
                    done = true;
                } catch (SocketTimeoutException ignore) {
                }
            }

            if (done) {
                channelOutboundBuffer.remove();
            }
        }

        @Override
        public void closeForcibly() {
            try {
                javaChannel().close();
                closeStreams();
            } catch (IOException ignore) {
            }
        }

        private void closeStreams() throws IOException {
            inputStream.close();
            outputStream.close();
        }

        @Override
        public SocketAddress getLocalAddress() throws Exception {
            return javaChannel().getLocalSocketAddress();
        }

        @Override
        public SocketAddress getRemoteAddress() throws Exception {
            return javaChannel().getRemoteSocketAddress();
        }
    }
}
