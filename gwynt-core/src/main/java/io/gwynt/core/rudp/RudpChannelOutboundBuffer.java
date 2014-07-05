package io.gwynt.core.rudp;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelOutboundBuffer;

final class RudpChannelOutboundBuffer extends ChannelOutboundBuffer {


    public RudpChannelOutboundBuffer(Channel channel) {
        super(channel);
    }

    @Override
    protected Object prepareMessage(Object message) {
        return super.prepareMessage(message);
    }
}
