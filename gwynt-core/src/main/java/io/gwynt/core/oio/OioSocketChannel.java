package io.gwynt.core.oio;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.RecvByteBufferAllocator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;

public class OioSocketChannel extends AbstractOioChannel {

    public OioSocketChannel(Channel parent, Object ch) {
        super(parent, ch);
    }

    @Override
    protected Unsafe newUnsafe() {
        return new OioSocketChannelUnsafe();
    }

    private class OioSocketChannelUnsafe extends AbstractOioUnsafe<Socket> {

        private ChannelPromise connectPromise;

        @Override
        protected boolean isActive() {
            return isOpen() && javaChannel().isConnected();
        }

        @Override
        protected boolean isOpen() {
            return !javaChannel().isClosed();
        }

        @Override
        public void connect(InetSocketAddress address, ChannelPromise channelPromise) {
            if (!channelPromise.setUncancellable()) {
                return;
            }
            connectPromise = channelPromise;
            try {
                javaChannel().connect(address);
                safeSetSuccess(connectPromise);
                pipeline().fireOpen();
            } catch (IOException e) {
                safeSetFailure(connectPromise, e);
            }
        }

        @Override
        protected void doDisconnect() {
            connectPromise = null;
            close(voidPromise());
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
                bytesRead = javaChannel().getInputStream().read(buffer);
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
        protected void doWriteMessages(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
            boolean done = false;
            Object message = channelOutboundBuffer.current();
            if (message != null) {
                try {
                    javaChannel().getOutputStream().write((byte[]) message);
                    done = true;
                } catch (SocketTimeoutException ignore) {
                }
            }

            if (done) {
                channelOutboundBuffer.remove();
            } else {
                writeRequested();
            }
        }

        @Override
        protected void closeJavaChannel() {
            try {
                javaChannel().close();
            } catch (IOException ignore) {
            }
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
