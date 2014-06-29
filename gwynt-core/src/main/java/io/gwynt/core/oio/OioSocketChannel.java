package io.gwynt.core.oio;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.RecvByteBufferAllocator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
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
            try {
                javaChannel().connect(address);
                safeSetSuccess(channelPromise);
                pipeline().fireOpen();
            } catch (IOException e) {
                safeSetFailure(channelPromise, e);
            }
        }

        @Override
        public void disconnect(ChannelPromise channelPromise) {
            super.disconnect(channelPromise);
        }

        @Override
        protected int doReadMessages(List<Object> messages) {
            if (!isActive()) {
                return 0;
            }

            RecvByteBufferAllocator.Handle allocHandle = allocHandle();
            ByteBuffer buffer = config().getByteBufferPool().acquire(allocHandle.guess(), false);
            Throwable error = null;
            int bytesRead = 0;
            int messagesRead = 0;

            try {
                bytesRead = javaChannel().getInputStream().read(buffer.array());
                if (bytesRead > 0) {
                    buffer.flip();
                    byte[] message = new byte[buffer.limit()];
                    buffer.get(message);
                    messages.add(message);
                    messagesRead++;
                }
            } catch (SocketTimeoutException ignore) {
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
            if (config().isAutoRead()) {
                readRequested();
            }
            return messagesRead;
        }

        @Override
        protected void doWriteMessages(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
            boolean done = false;
            Object message = channelOutboundBuffer.current();
            if (message != null) {
                for (int i = 0; i < config().getWriteSpinCount(); i++) {
                    try {
                        javaChannel().getOutputStream().write((byte[]) message);
                        done = true;
                    } catch (SocketTimeoutException ignore) {
                    }

                    if (done) {
                        break;
                    }
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
    }
}
