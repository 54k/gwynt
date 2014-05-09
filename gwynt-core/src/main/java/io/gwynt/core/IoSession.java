package io.gwynt.core;

import io.gwynt.core.pipeline.Pipeline;

import java.io.Closeable;
import java.net.SocketAddress;

public interface IoSession extends Closeable {

    void write(byte[] data);

    void close();

    boolean isClosed();

    boolean isPendingClose();

    Object attach(Object attachment);

    Object attachment();

    Pipeline getPipeline();

    Endpoint getEndpoint();

    SocketAddress getLocalAddress();

    SocketAddress getRemoteAddress();
}
