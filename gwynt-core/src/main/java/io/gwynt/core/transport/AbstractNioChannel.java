package io.gwynt.core.transport;

import io.gwynt.core.Endpoint;

import java.nio.channels.SelectableChannel;

public abstract class AbstractNioChannel extends AbstractChannel {

    protected AbstractNioUnsafe unsafe;

    protected AbstractNioChannel(Endpoint endpoint) {
        this(null, endpoint);
    }

    protected AbstractNioChannel(AbstractNioChannel parent, Endpoint endpoint) {
        super(parent, endpoint);
    }

    @Override
    protected boolean isDispatcherCompatible(Dispatcher dispatcher) {
        return dispatcher instanceof NioEventLoop;
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    protected abstract class AbstractNioUnsafe<T extends SelectableChannel> extends AbstractUnsafe<T> {

        protected AbstractNioUnsafe(T ch) {
            super(ch);
        }
    }
}
