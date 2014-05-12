package io.gwynt.core.transport.udp;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

final class Datagram {

    private SocketAddress recipient;
    private ByteBuffer message;

    public Datagram(SocketAddress recipient, ByteBuffer message) {
        this.recipient = recipient;
        this.message = message;
    }

    public SocketAddress getRecipient() {
        return recipient;
    }

    public ByteBuffer getMessage() {
        return message;
    }
}
