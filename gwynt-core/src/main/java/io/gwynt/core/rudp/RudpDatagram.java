package io.gwynt.core.rudp;

import io.gwynt.core.Datagram;

final class RudpDatagram extends Datagram {

    private boolean reliable;

    public RudpDatagram(byte[] content, boolean reliable) {
        super(content, null);
        this.reliable = reliable;
    }

    public boolean isReliable() {
        return reliable;
    }

    public void setReliable(boolean reliable) {
        this.reliable = reliable;
    }
}
