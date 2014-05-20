package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.exception.EofException;
import io.gwynt.core.util.ByteBufferAllocator;
import io.gwynt.core.util.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.List;

public class NioDatagramChannel extends AbstractNioChannel {

    @SuppressWarnings("unused")
    public NioDatagramChannel(Endpoint endpoint) throws IOException {
        this(null, endpoint);
    }

    public NioDatagramChannel(AbstractNioChannel parent, Endpoint endpoint) throws IOException {
        super(parent, endpoint, DatagramChannel.open());
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
        protected void doAfterRegister() {
            super.doAfterRegister();
            interestOps(interestOps() | SelectionKey.OP_READ);
        }

        @Override
        protected void doAcceptChannels(List<Pair<Channel, ChannelPromise>> channels) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doReadMessages(List<Object> messages) {
            ByteBuffer buffer = ByteBufferAllocator.allocate(150000);
            SocketAddress address;

            do {
                try {
                    address = javaChannel().receive(buffer);
                    if (address != null) {
                        buffer.flip();
                        byte[] message = new byte[buffer.limit()];
                        buffer.get(message);
                        messages.add(new Datagram(address, ByteBuffer.wrap(message)));
                    }
                } catch (IOException e) {
                    throw new EofException();
                }
            } while (buffer.hasRemaining() && address != null);

            ByteBufferAllocator.release(buffer);
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
