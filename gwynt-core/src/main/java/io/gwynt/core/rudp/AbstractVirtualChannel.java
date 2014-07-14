package io.gwynt.core.rudp;

import io.gwynt.core.AbstractChannel;
import io.gwynt.core.Channel;

public abstract class AbstractVirtualChannel extends AbstractChannel {

    protected AbstractVirtualChannel(Channel parent) {
        super(parent, null);
    }

    public static interface VirtualUnsafe<T> extends Unsafe<T> {

        void messageReceived(Object message);
    }

    protected abstract class AbstractVirtualUnsafe<T> extends AbstractUnsafe<T> implements VirtualUnsafe<T> {

    }
}
