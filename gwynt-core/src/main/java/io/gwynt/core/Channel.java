package io.gwynt.core;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface Channel<T> {

    T unwrap();

    int read(ByteBuffer dst) throws IOException;

    int write(ByteBuffer src) throws IOException;

    void close() throws IOException;

    SocketAddress getLocalAddress();

    SocketAddress getRemoteAddress();
}
