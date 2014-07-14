package io.gwynt.core.oio;

import io.gwynt.core.ChannelException;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Datagram;
import io.gwynt.core.DefaultChannelPromise;
import io.gwynt.core.Envelope;
import io.gwynt.core.MulticastChannel;
import io.gwynt.core.buffer.RecvByteBufferAllocator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.List;

public class OioDatagramChannel extends AbstractOioChannel implements MulticastChannel {

    @SuppressWarnings("unused")
    public OioDatagramChannel() throws IOException {
        super(new MulticastSocket());
    }

    @Override
    protected Unsafe newUnsafe() {
        return new OioDatagramChannelUnsafe();
    }

    @Override
    protected MulticastSocket javaChannel() {
        return (MulticastSocket) super.javaChannel();
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress) {
        return joinGroup(multicastAddress, newChannelPromise());
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return new DefaultChannelPromise(this, eventLoop()).setFailure(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, ChannelPromise channelPromise) {
        if (multicastAddress == null) {
            throw new IllegalArgumentException("multicastAddress");
        }

        try {
            javaChannel().joinGroup(multicastAddress);
            safeSetSuccess(channelPromise);
        } catch (Throwable e) {
            safeSetFailure(channelPromise, e);
        }

        return channelPromise;
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise channelPromise) {
        if (multicastAddress == null) {
            throw new IllegalArgumentException("multicastAddress");
        }

        if (networkInterface == null) {
            throw new IllegalArgumentException("networkInterface");
        }

        try {
            javaChannel().joinGroup(multicastAddress, networkInterface);
            safeSetSuccess(channelPromise);
        } catch (Throwable e) {
            safeSetFailure(channelPromise, e);
        }

        return channelPromise;
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return new DefaultChannelPromise(this, eventLoop()).setFailure(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        safeSetFailure(channelPromise, new UnsupportedOperationException());
        return channelPromise;
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress) {
        return leaveGroup(multicastAddress, newChannelPromise());
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return leaveGroup(multicastAddress, networkInterface, newChannelPromise());
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, ChannelPromise channelPromise) {
        if (multicastAddress == null) {
            throw new IllegalArgumentException("multicastAddress");
        }

        try {
            javaChannel().leaveGroup(multicastAddress);
            safeSetSuccess(channelPromise);
        } catch (IOException e) {
            safeSetFailure(channelPromise, e);
        }
        return channelPromise;
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise channelPromise) {
        if (multicastAddress == null) {
            throw new IllegalArgumentException("multicastAddress");
        }
        if (networkInterface == null) {
            throw new IllegalArgumentException("networkInterface");
        }

        try {
            javaChannel().leaveGroup(multicastAddress, networkInterface);
            safeSetSuccess(channelPromise);
        } catch (IOException e) {
            safeSetFailure(channelPromise, e);
        }
        return channelPromise;
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return new DefaultChannelPromise(this, eventLoop()).setFailure(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        safeSetFailure(channelPromise, new UnsupportedOperationException());
        return channelPromise;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress source) {
        return new DefaultChannelPromise(this, eventLoop()).setFailure(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress source, ChannelPromise channelPromise) {
        safeSetFailure(channelPromise, new UnsupportedOperationException());
        return channelPromise;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return new DefaultChannelPromise(this, eventLoop()).setFailure(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        safeSetFailure(channelPromise, new UnsupportedOperationException());
        return channelPromise;
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, InetAddress source) {
        return new DefaultChannelPromise(this, eventLoop()).setFailure(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, InetAddress source, ChannelPromise channelPromise) {
        safeSetFailure(channelPromise, new UnsupportedOperationException());
        return channelPromise;
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return new DefaultChannelPromise(this, eventLoop()).setFailure(new UnsupportedOperationException());
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        safeSetFailure(channelPromise, new UnsupportedOperationException());
        return channelPromise;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) super.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    private class OioDatagramChannelUnsafe extends AbstractOioUnsafe<DatagramSocket> {

        @Override
        public boolean isActive() {
            return isOpen() && (javaChannel().isBound() || javaChannel().isConnected());
        }

        @Override
        public boolean isOpen() {
            return !javaChannel().isClosed();
        }

        @Override
        protected void doConnect(InetSocketAddress address, ChannelPromise channelPromise) throws Exception {
            javaChannel().connect(address);
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
            ByteBuffer buffer = config().getByteBufferPool().acquire(allocHandle.guess(), false);
            SocketAddress address;

            try {
                byte[] array = buffer.array();
                DatagramPacket datagramPacket = new DatagramPacket(array, array.length);
                javaChannel().receive(datagramPacket);
                address = datagramPacket.getSocketAddress();

                if (address != null) {
                    buffer.flip();
                    byte[] message = new byte[buffer.limit()];
                    buffer.get(message);
                    messages.add(new Datagram(message, address));
                    return 1;
                }
            } catch (SocketTimeoutException ignore) {
            } finally {
                config().getByteBufferPool().release(buffer);
            }

            return 0;
        }

        @Override
        protected void doWrite(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
            boolean done = false;
            Object message = channelOutboundBuffer.current();
            if (message != null) {
                done = doWriteMessage(message);
            }

            if (done) {
                channelOutboundBuffer.remove();
            }
        }

        @SuppressWarnings("unchecked")
        protected boolean doWriteMessage(Object message) throws Exception {
            byte[] src;
            SocketAddress remoteAddress;

            if (message instanceof Envelope) {
                Envelope<byte[], SocketAddress> envelope = (Envelope<byte[], SocketAddress>) message;
                src = envelope.content();
                remoteAddress = envelope.recipient();
            } else if (message instanceof byte[]) {
                src = (byte[]) message;
                remoteAddress = null;
            } else {
                throw new ChannelException("Unsupported message type: " + message.getClass().getSimpleName());
            }

            if (remoteAddress != null) {
                javaChannel().send(new DatagramPacket(src, src.length, remoteAddress));
            } else {
                javaChannel().send(new DatagramPacket(src, src.length, javaChannel().getRemoteSocketAddress()));
            }

            return true;
        }

        @Override
        public void closeForcibly() {
            javaChannel().close();
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
