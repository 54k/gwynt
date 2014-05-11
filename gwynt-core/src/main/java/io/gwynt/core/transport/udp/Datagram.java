package io.gwynt.core.transport.udp;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

final class Datagram {

    private SocketAddress address;
    private ByteBuffer message;

    public Datagram(SocketAddress address, ByteBuffer message) {
        this.address = address;
        this.message = message;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public ByteBuffer getMessage() {
        return message;
    }
}
