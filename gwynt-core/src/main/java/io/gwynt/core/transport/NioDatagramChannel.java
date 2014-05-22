package io.gwynt.core.transport;

import io.gwynt.core.ChannelPromise;
import io.gwynt.core.exception.EofException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.List;

public class NioDatagramChannel extends AbstractNioChannel {

    @SuppressWarnings("unused")
    public NioDatagramChannel() throws IOException {
        this(null);
    }

    public NioDatagramChannel(AbstractNioChannel parent) throws IOException {
        super(parent, DatagramChannel.open());
    }

    @Override
    protected Unsafe newUnsafe() {
        return new NioDatagramChannelUnsafe();
    }

    private class NioDatagramChannelUnsafe extends AbstractNioUnsafe<DatagramChannel> {

        @Override
        protected void closeRequested() {
            interestOps(SelectionKey.OP_WRITE);
        }

        @Override
        public void bind(InetSocketAddress address, ChannelPromise channelPromise) {
            try {
                javaChannel().bind(address);
                channelPromise.complete();
                pipeline().fireOpen();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void connect(InetSocketAddress address, ChannelPromise channelPromise) {
            try {
                javaChannel().connect(address);
                channelPromise.complete();
                pipeline().fireOpen();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void afterRegister() {
            super.afterRegister();
            if (config().isAutoRead()) {
                interestOps(interestOps() | SelectionKey.OP_READ);
            }
        }

        @Override
        protected int doReadMessages(List<Object> messages) {
            ByteBuffer buffer = config().getByteBufferPool().acquire(4096, true);
            SocketAddress address;
            int messagesRead = 0;
            do {
                try {
                    address = javaChannel().receive(buffer);
                    if (address != null) {
                        buffer.flip();
                        byte[] message = new byte[buffer.limit()];
                        buffer.get(message);
                        messages.add(new Datagram(address, ByteBuffer.wrap(message)));
                        messagesRead++;
                    }
                } catch (IOException e) {
                    exceptionCaught(e);
                    config().getByteBufferPool().release(buffer);
                    return messagesRead;
                }
            } while (buffer.hasRemaining() && address != null);

            config().getByteBufferPool().release(buffer);
            return messagesRead;
        }

        @Override
        protected boolean doWriteMessage(Object message) {
            int bytesWritten;
            Datagram datagram = (Datagram) message;
            ByteBuffer src = datagram.getMessage();
            do {
                try {
                    bytesWritten = javaChannel().send(src, datagram.getRecipient());
                } catch (IOException e) {
                    throw new EofException();
                }
            } while (src.hasRemaining() && bytesWritten > 0);

            if (bytesWritten == -1) {
                throw new EofException();
            }

            return !src.hasRemaining();
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
