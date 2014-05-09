package io.gwynt.core;

import io.gwynt.core.pipeline.DefaultPipeline;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractIoSession<T> implements IoSession {

    protected final ByteBuffer readBuffer = ByteBuffer.allocateDirect(4096);

    protected final AtomicBoolean closed = new AtomicBoolean(false);
    protected final AtomicBoolean pendingClose = new AtomicBoolean(false);

    protected final AtomicReference<Object> attachment = new AtomicReference<>();
    protected final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();

    protected Channel<T> channel;
    protected DefaultPipeline pipeline;
    protected Endpoint endpoint;

    protected AbstractIoSession(Channel<T> channel, Endpoint endpoint) {
        this.channel = channel;
        this.endpoint = endpoint;

        pipeline = new DefaultPipeline(this);
        for (IoHandler ioHandler : endpoint.getHandlers()) {
            pipeline.addHandler(ioHandler);
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public boolean isPendingClose() {
        return pendingClose.get();
    }

    @Override
    public Object attach(Object attachment) {
        return this.attachment.getAndSet(attachment);
    }

    @Override
    public Object attachment() {
        return attachment.get();
    }

    @Override
    public DefaultPipeline getPipeline() {
        return pipeline;
    }

    @Override
    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }
}
