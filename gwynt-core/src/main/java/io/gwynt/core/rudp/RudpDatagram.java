package io.gwynt.core.rudp;

import io.gwynt.core.Datagram;

import java.net.SocketAddress;

public final class RudpDatagram extends Datagram {

    public RudpDatagram(byte[] content, SocketAddress recipient) {
        super(content, recipient);
    }
}
