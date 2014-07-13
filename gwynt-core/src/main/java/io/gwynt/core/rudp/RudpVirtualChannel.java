package io.gwynt.core.rudp;

import io.gwynt.core.Channel;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class RudpVirtualChannel implements Channel {

    protected abstract class RudpVirualChannelUnsafe<T> implements Unsafe<T> {

        private AtomicInteger localSequence = new AtomicInteger();
        private AtomicInteger remoteSequence = new AtomicInteger();

    }
}
