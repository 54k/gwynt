package io.gwynt.core;

import java.net.SocketAddress;

public class DefaultEnvelope<V, A extends SocketAddress> implements Envelope<V, A> {

    private V content;
    private A recipient;
    private A sender;

    public DefaultEnvelope(V content, A recipient, A sender) {
        this.content = content;
        this.recipient = recipient;
        this.sender = sender;
    }

    public DefaultEnvelope(V content, A recipient) {
        this(content, recipient, null);
    }

    @Override
    public V content() {
        return null;
    }

    @Override
    public A recipient() {
        return null;
    }

    @Override
    public A sender() {
        return null;
    }

}
