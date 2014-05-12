package io.gwynt.core;

import io.gwynt.core.pipeline.DefaultPipeline;
import io.gwynt.core.transport.Dispatcher;
import io.gwynt.core.transport.tcp.SelectorEventListener;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractIoSession<T> implements SelectorEventListener, IoSession {

    protected final ByteBuffer readBuffer = ByteBuffer.allocateDirect(150000);
    protected final Object registrationLock = new Object();
    protected final AtomicBoolean registered = new AtomicBoolean(false);
    protected final AtomicReference<Dispatcher> dispatcher = new AtomicReference<>();

    protected final AtomicReference<IoSessionStatus> status = new AtomicReference<>(IoSessionStatus.CLOSED);
    protected final AtomicReference<Object> attachment = new AtomicReference<>();
    protected final Queue<Object> writeQueue = new ConcurrentLinkedQueue<>();

    protected Channel<T> channel;
    protected DefaultPipeline pipeline;
    protected Endpoint endpoint;

    protected AbstractIoSession(Channel<T> channel, Endpoint endpoint) {
        this.channel = channel;
        this.endpoint = endpoint;

        pipeline = new DefaultPipeline(this);
        for (IoHandler ioHandler : endpoint.getHandlers()) {
            pipeline.addLast(ioHandler);
        }
    }

    @Override
    public boolean isRegistered() {
        return registered.get();
    }

    @Override
    public IoSessionStatus getStatus() {
        return status.get();
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
