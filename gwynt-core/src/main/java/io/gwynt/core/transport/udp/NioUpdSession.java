package io.gwynt.core.transport.udp;

import io.gwynt.core.AbstractIoSession;
import io.gwynt.core.Endpoint;
import io.gwynt.core.IoSessionStatus;
import io.gwynt.core.transport.Channel;
import io.gwynt.core.transport.Dispatcher;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class NioUpdSession extends AbstractIoSession<DatagramChannel> {

    private Map<SocketAddress, InternalNioUdpSession> address2session = Collections.synchronizedMap(new WeakHashMap<SocketAddress, InternalNioUdpSession>());

    public NioUpdSession(Channel<DatagramChannel> channel, Endpoint endpoint) {
        super(channel, endpoint);
    }

    @Override
    public void write(Object data) {
        if (!(data instanceof Datagram)) {
            throw new IllegalArgumentException("Data is not instanceof " + Datagram.class.getCanonicalName());
        }
        if (status.get() != IoSessionStatus.PENDING_CLOSE && status.get() != IoSessionStatus.CLOSED) {
            writeQueue.add(data);
            synchronized (registrationLock) {
                if (registered.get()) {
                    dispatcher.get().modifyRegistration(channel.unwrap(), SelectionKey.OP_WRITE);
                }
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void onSessionUnregistered(Dispatcher dispatcher) {
        synchronized (registrationLock) {
            registered.set(false);
            this.dispatcher.set(null);
            pipeline.fireUnregistered();
        }
        status.set(IoSessionStatus.CLOSED);
        pipeline.fireClose();
    }

    @Override
    public void onSessionRegistered(Dispatcher dispatcher) {
        synchronized (registrationLock) {
            registered.set(true);
            this.dispatcher.set(dispatcher);
            pipeline.fireRegistered();
        }
        status.set(IoSessionStatus.OPENED);
        pipeline.fireOpen();
    }

    @Override
    public void onSelectedForRead(SelectionKey key) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        SocketAddress address = channel.receive(readBuffer);

        if (!address2session.containsKey(address)) {
            InternalNioUdpSession session = new InternalNioUdpSession(this, address);
            address2session.put(address, session);
        }

        readBuffer.flip();
        byte[] message = new byte[readBuffer.limit()];
        readBuffer.get(message);
        readBuffer.clear();
        address2session.get(address).fireMessageReceived(message);
    }

    @Override
    public void onSelectedForWrite(SelectionKey key) throws IOException {
        Datagram data = (Datagram) writeQueue.peek();

        if (data != null) {
            DatagramChannel channel = (DatagramChannel) key.channel();
            channel.send(data.getMessage(), data.getRecipient());
            if (!data.getMessage().hasRemaining()) {
                writeQueue.poll();
            }
        }

        if (!writeQueue.isEmpty()) {
            dispatcher.get().modifyRegistration(channel.unwrap(), SelectionKey.OP_WRITE);
        }
    }

    @Override
    public void onExceptionCaught(Throwable e) {
        pipeline.fireExceptionCaught(e);
    }

    private static class InternalNioUdpSession extends NioUpdSession {

        private NioUpdSession parent;
        private SocketAddress recipient;

        private InternalNioUdpSession(NioUpdSession parent, SocketAddress recipient) {
            super(parent.channel, parent.endpoint);
            this.parent = parent;
            this.recipient = recipient;
        }

        @Override
        public void write(Object data) {
            parent.write(new Datagram(recipient, ByteBuffer.wrap((byte[]) data)));
        }

        @Override
        public void close() {
            parent.close();
        }

        @Override
        public boolean isRegistered() {
            return parent.isRegistered();
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return recipient;
        }

        void fireMessageReceived(byte[] message) {
            pipeline.fireMessageReceived(message);
        }
    }
}
