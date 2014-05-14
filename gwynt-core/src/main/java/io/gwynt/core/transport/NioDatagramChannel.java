package io.gwynt.core.transport;

import io.gwynt.core.Endpoint;
import io.gwynt.core.exception.EofException;
import io.gwynt.core.util.ByteBufferAllocator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.List;

public class NioDatagramChannel extends AbstractNioChannel {

    public NioDatagramChannel(Endpoint endpoint) {
        super(endpoint);
        try {
            unsafe = new NioDatagramUnsafe(DatagramChannel.open());
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    private class NioDatagramUnsafe extends AbstractUnsafe<DatagramChannel> {

        private NioDatagramUnsafe(DatagramChannel ch) {
            super(ch);
        }

        @Override
        public void bind(InetSocketAddress address) {
            try {
                javaChannel().bind(address);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void connect(InetSocketAddress address) {
            try {
                javaChannel().connect(address);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void doRegister0() {
            pipeline().fireRegistered();
            if (isActive()) {
                pipeline().fireOpen();
            }
            dispatcher().modifyRegistration(NioDatagramChannel.this, SelectionKey.OP_READ);
        }

        @Override
        protected void doUnregister0() {
            pipeline().fireUnregistered();
            if (!isActive()) {
                pipeline().fireClose();
            }
        }

        @Override
        protected void doAccept0(List<AbstractNioChannel> channels) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doRead0(List<Object> messages) {
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
                    throw new RuntimeException(e);
                }
            } while (buffer.hasRemaining() && address != null);

            ByteBufferAllocator.release(buffer);
        }

        @Override
        protected boolean doWrite0(Object message) {
            int bytesWritten;
            Datagram datagram = (Datagram) message;
            ByteBuffer src = datagram.getMessage();
            do {
                try {
                    bytesWritten = javaChannel().send(src, datagram.getRecipient());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } while (src.hasRemaining() && bytesWritten > 0);

            if (bytesWritten == -1) {
                throw new EofException();
            }

            return !src.hasRemaining();
        }
    }
}
