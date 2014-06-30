package io.gwynt.core.oio;

import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.ServerChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

public class OioServerSocketChannel extends AbstractOioChannel implements ServerChannel {

    @SuppressWarnings("unused")
    public OioServerSocketChannel() throws IOException {
        super(new ServerSocket());
    }

    @Override
    protected Unsafe newUnsafe() {
        return new OioServerSocketChannelUnsafe();
    }

    private class OioServerSocketChannelUnsafe extends AbstractOioUnsafe<ServerSocket> {

        @Override
        protected boolean isActive() {
            return isOpen() && javaChannel().isBound();
        }

        @Override
        protected boolean isOpen() {
            return !javaChannel().isClosed();
        }

        @Override
        public void bind(InetSocketAddress address, ChannelPromise channelPromise) {
            try {
                javaChannel().bind(address);
                safeSetSuccess(channelPromise);
                pipeline().fireOpen();
                if (config().isAutoRead()) {
                    readRequested();
                }
            } catch (IOException e) {
                safeSetFailure(channelPromise, e);
                doClose();
            }
        }

        @Override
        public void write(Object message, ChannelPromise channelPromise) {
            safeSetFailure(channelPromise, new UnsupportedOperationException());
        }

        @Override
        protected int doReadMessages(List<Object> messages) {
            if (!isActive()) {
                return 0;
            }

            Socket ch;
            int accepted = 0;
            try {
                ch = javaChannel().accept();
                if (ch == null) {
                    return 0;
                }
                ch.setSoTimeout(SO_TIMEOUT);
                accepted++;
            } catch (SocketTimeoutException ignore) {
                return 0;
            } catch (IOException e) {
                exceptionCaught(e);
                return 0;
            } finally {
                if (config().isAutoRead()) {
                    readRequested();
                }
            }

            OioSocketChannel channel = new OioSocketChannel(OioServerSocketChannel.this, ch);
            messages.add(channel);

            return accepted;
        }

        @Override
        protected void doWriteMessages(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
            throw new UnsupportedOperationException();
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
