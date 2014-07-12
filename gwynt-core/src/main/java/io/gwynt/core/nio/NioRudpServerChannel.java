package io.gwynt.core.nio;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelConfig;
import io.gwynt.core.ChannelException;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Datagram;
import io.gwynt.core.Envelope;
import io.gwynt.core.MulticastChannel;
import io.gwynt.core.ServerChannel;
import io.gwynt.core.buffer.RecvByteBufferAllocator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NioRudpServerChannel extends AbstractNioChannel implements ServerChannel, MulticastChannel {

    private Map<InetAddress, List<MembershipKey>> memberships = new HashMap<>();

    @SuppressWarnings("unused")
    public NioRudpServerChannel() throws IOException {
        this(null);
    }

    public NioRudpServerChannel(AbstractNioChannel parent) throws IOException {
        super(parent, DatagramChannel.open());
    }

    @Override
    protected Unsafe newUnsafe() {
        return new NioRudpServerChannelUnsafe();
    }

    @Override
    protected ChannelConfig newConfig() {
        return new RudpChannelConfig(this);
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress) {
        return joinGroup(multicastAddress, newChannelPromise());
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
        return joinGroup(multicastAddress, networkInterface, newChannelPromise());
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, ChannelPromise channelPromise) {
        try {
            return joinGroup(multicastAddress, NetworkInterface.getByInetAddress(getLocalAddress().getAddress()), null, newChannelPromise());
        } catch (SocketException e) {
            safeSetFailure(channelPromise, e);
        }
        return channelPromise;
    }

    @Override
    public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise channelPromise) {
        return joinGroup(multicastAddress.getAddress(), networkInterface, null, channelPromise);
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return joinGroup(multicastAddress, networkInterface, source, newChannelPromise());
    }

    @Override
    public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        if (multicastAddress == null) {
            throw new IllegalArgumentException("multicastAddress");
        }

        if (networkInterface == null) {
            throw new IllegalArgumentException("networkInterface");
        }

        try {
            MembershipKey key;
            if (source == null) {
                key = javaChannel().join(multicastAddress, networkInterface);
            } else {
                key = javaChannel().join(multicastAddress, networkInterface, source);
            }

            synchronized (this) {
                List<MembershipKey> keys = null;
                if (memberships == null) {
                    memberships = new HashMap<>();
                } else {
                    keys = memberships.get(multicastAddress);
                }
                if (keys == null) {
                    keys = new ArrayList<>();
                    memberships.put(multicastAddress, keys);
                }
                keys.add(key);
            }

            safeSetSuccess(channelPromise);
        } catch (Throwable e) {
            safeSetFailure(channelPromise, e);
        }

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
        try {
            return leaveGroup(multicastAddress, NetworkInterface.getByInetAddress(getLocalAddress().getAddress()), null, channelPromise);
        } catch (SocketException e) {
            safeSetFailure(channelPromise, e);
        }
        return channelPromise;
    }

    @Override
    public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise channelPromise) {
        return leaveGroup(multicastAddress.getAddress(), networkInterface, null, channelPromise);
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return leaveGroup(multicastAddress, networkInterface, source, newChannelPromise());
    }

    @Override
    public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        if (multicastAddress == null) {
            throw new IllegalArgumentException("multicastAddress");
        }
        if (networkInterface == null) {
            throw new IllegalArgumentException("networkInterface");
        }

        synchronized (this) {
            if (memberships != null) {
                List<MembershipKey> keys = memberships.get(multicastAddress);
                if (keys != null) {
                    Iterator<MembershipKey> keyIt = keys.iterator();

                    while (keyIt.hasNext()) {
                        MembershipKey key = keyIt.next();
                        if (networkInterface.equals(key.networkInterface())) {
                            if (source == null && key.sourceAddress() == null || source != null && source.equals(key.sourceAddress())) {
                                key.drop();
                                keyIt.remove();
                            }
                        }
                    }
                    if (keys.isEmpty()) {
                        memberships.remove(multicastAddress);
                    }
                }
            }
        }

        safeSetSuccess(channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress source) {
        return block(multicastAddress, source, newChannelPromise());
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, InetAddress source, ChannelPromise channelPromise) {
        try {
            return block(multicastAddress, NetworkInterface.getByInetAddress(getLocalAddress().getAddress()), source, channelPromise);
        } catch (SocketException e) {
            safeSetFailure(channelPromise, e);
        }
        return channelPromise;
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return block(multicastAddress, networkInterface, source, newChannelPromise());
    }

    @Override
    public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        if (multicastAddress == null) {
            throw new IllegalArgumentException("multicastAddress");
        }
        if (source == null) {
            throw new IllegalArgumentException("sourceToBlock");
        }
        if (networkInterface == null) {
            throw new IllegalArgumentException("networkInterface");
        }

        synchronized (this) {
            if (memberships != null) {
                List<MembershipKey> keys = memberships.get(multicastAddress);
                for (MembershipKey key : keys) {
                    if (networkInterface.equals(key.networkInterface())) {
                        try {
                            key.block(source);
                        } catch (IOException e) {
                            safeSetFailure(channelPromise, e);
                        }
                    }
                }
            }
        }
        safeSetSuccess(channelPromise);
        return channelPromise;
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, InetAddress source) {
        return unblock(multicastAddress, source, newChannelPromise());
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, InetAddress source, ChannelPromise channelPromise) {
        try {
            return unblock(multicastAddress, NetworkInterface.getByInetAddress(getLocalAddress().getAddress()), source, channelPromise);
        } catch (SocketException e) {
            safeSetFailure(channelPromise, e);
        }
        return channelPromise;
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
        return unblock(multicastAddress, networkInterface, source, newChannelPromise());
    }

    @Override
    public ChannelFuture unblock(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise channelPromise) {
        if (multicastAddress == null) {
            throw new IllegalArgumentException("multicastAddress");
        }
        if (source == null) {
            throw new IllegalArgumentException("sourceToBlock");
        }
        if (networkInterface == null) {
            throw new IllegalArgumentException("networkInterface");
        }

        synchronized (this) {
            if (memberships != null) {
                List<MembershipKey> keys = memberships.get(multicastAddress);
                for (MembershipKey key : keys) {
                    if (networkInterface.equals(key.networkInterface())) {
                        key.unblock(source);
                    }
                }
            }
        }
        safeSetSuccess(channelPromise);
        return channelPromise;
    }

    @Override
    protected DatagramChannel javaChannel() {
        return (DatagramChannel) super.javaChannel();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) super.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    @Override
    public RudpChannelConfig config() {
        return (RudpChannelConfig) super.config();
    }

    private class NioRudpServerChannelUnsafe extends AbstractNioUnsafe<DatagramChannel> {

        private Map<SocketAddress, Channel> clients = new HashMap<>();

        @Override
        protected boolean isActive() {
            return javaChannel().isOpen() && (javaChannel().socket().isBound() || javaChannel().isConnected());
        }

        @Override
        protected void doBind(InetSocketAddress address, ChannelPromise channelPromise) throws Exception {
            javaChannel().bind(address);
        }

        @Override
        protected boolean doConnect(InetSocketAddress address, ChannelPromise channelPromise) throws Exception {
            javaChannel().connect(address);
            return true;
        }

        @Override
        protected boolean doFinishConnect() throws Exception {
            throw new Error();
        }

        @Override
        public void doDisconnect(ChannelPromise channelPromise) throws Exception {
            javaChannel().disconnect();
        }

        @Override
        protected void afterRegister() {
            super.afterRegister();
            if (config().isAutoRead()) {
                interestOps(interestOps() | SelectionKey.OP_READ);
            }
        }

        @Override
        protected int doReadMessages(List<Object> messages) throws Exception {
            RecvByteBufferAllocator.Handle allocHandle = allocHandle();
            ByteBuffer buffer = allocHandle.allocate(config().getByteBufferPool());
            try {
                SocketAddress address = javaChannel().receive(buffer);
                if (address != null) {
                    buffer.flip();
                    byte[] message = new byte[buffer.limit()];
                    buffer.get(message);
                    messages.add(new Datagram(message, address));
                    return 1;
                }
            } finally {
                config().getByteBufferPool().release(buffer);
            }
            return 0;
        }

        private void processPacket(SocketAddress address, ByteBuffer packet) {
            if (!protocolMatching(packet)) {
                // discard
                return;
            }

            if (clients.containsKey(address)) {
                //processClient
            } else {
                //createClient
            }
        }

        private boolean protocolMatching(ByteBuffer packet) {
            return packet.remaining() > 4 && packet.getInt() == config().getProtocolId();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected boolean doWriteMessage(Object message) throws Exception {
            int bytesWritten;

            ByteBuffer src;
            SocketAddress remoteAddress;

            if (message instanceof Envelope) {
                Envelope<byte[], SocketAddress> envelope = (Envelope<byte[], SocketAddress>) message;
                byte[] bytes = envelope.content();
                src = byteBufferPool().acquire(bytes.length, false).put(bytes);
                src.flip();
                remoteAddress = envelope.recipient();
            } else if (message instanceof ByteBuffer) {
                src = (ByteBuffer) message;
                remoteAddress = null;
            } else if (message instanceof byte[]) {
                byte[] bytes = (byte[]) message;
                src = byteBufferPool().acquire(bytes.length, false).put(bytes);
                src.flip();
                remoteAddress = null;
            } else {
                throw new ChannelException("Unsupported message type: " + message.getClass().getSimpleName());
            }

            if (remoteAddress != null) {
                bytesWritten = javaChannel().send(src, remoteAddress);
            } else {
                bytesWritten = javaChannel().write(src);
            }

            return bytesWritten > 0;
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
