package io.gwynt.core;

import java.net.SocketAddress;

public class Datagram extends DefaultEnvelope<byte[], SocketAddress> {

    public Datagram(byte[] content, SocketAddress recipient, SocketAddress sender) {
        super(content, recipient, sender);
    }
}
