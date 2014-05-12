package io.gwynt.core.transport;

import java.nio.channels.SelectableChannel;

public abstract class AbstractSelectableChannel<T extends SelectableChannel> implements Channel<T> {

    protected T channel;

    protected AbstractSelectableChannel(T channel) {
        this.channel = channel;
    }

    @Override
    public T unwrap() {
        return channel;
    }
}
