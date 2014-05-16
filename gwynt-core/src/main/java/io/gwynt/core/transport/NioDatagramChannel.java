package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.exception.ChannelException;
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
    public NioDatagramChannel(Endpoint endpoint) {
        this(null, endpoint);
    }

    public NioDatagramChannel(AbstractNioChannel parent, Endpoint endpoint) {
        super(parent, endpoint);
        try {
            unsafe = new NioDatagramNioUnsafe(DatagramChannel.open());
        } catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public SocketAddress getLocalAddress() {
        try {
            return ((DatagramChannel) unsafe.javaChannel()).getLocalAddress();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public SocketAddress getRemoteAddress() {
        try {
            return ((DatagramChannel) unsafe.javaChannel()).getRemoteAddress();
        } catch (IOException e) {
            return null;
        }
    }

    private class NioDatagramNioUnsafe extends AbstractNioUnsafe<DatagramChannel> {

        private NioDatagramNioUnsafe(DatagramChannel ch) {
            super(ch);
        }

        @Override
        public ChannelFuture bind(InetSocketAddress address, ChannelPromise channelPromise) {
            try {
                javaChannel().bind(address);
                channelPromise.complete();
                return channelPromise;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ChannelFuture connect(InetSocketAddress address, ChannelPromise channelPromise) {
            try {
                javaChannel().connect(address);
                channelPromise.complete();
                pipeline().fireOpen();
                return channelPromise;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void doAfterRegister() {
            pipeline().fireRegistered();
            if (isActive()) {
                pipeline().fireOpen();
            }
            dispatcher().modifyRegistration(NioDatagramChannel.this, SelectionKey.OP_READ);
        }

        @Override
        protected void doAfterUnregister() {
            pipeline().fireUnregistered();
            if (!isActive()) {
                pipeline().fireClose();
            }
        }

        @Override
        protected void doAcceptImpl(List<Pair<Channel, ChannelPromise>> channels) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doReadImpl(List<Object> messages) {
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
        protected boolean doWriteImpl(Object message) {
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
    }
}
