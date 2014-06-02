package io.gwynt.core;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class Datagram extends DefaultEnvelope<ByteBuffer, SocketAddress> {

    public Datagram(ByteBuffer content, SocketAddress recipient) {
        super(content, recipient);
    }
}
