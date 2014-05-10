package io.gwynt.core;

import io.gwynt.core.pipeline.Pipeline;

import java.io.Closeable;
import java.net.SocketAddress;

public interface IoSession extends Closeable {

    void write(byte[] data);

    void close();

    IoSessionStatus getStatus();

    boolean isRegistered();

    Object attach(Object attachment);

    Object attachment();

    Pipeline getPipeline();

    Endpoint getEndpoint();

    SocketAddress getLocalAddress();

    SocketAddress getRemoteAddress();
}
