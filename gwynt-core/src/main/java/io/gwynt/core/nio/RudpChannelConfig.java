package io.gwynt.core.nio;

import io.gwynt.core.Channel;
import io.gwynt.core.DefaultChannelConfig;

public class RudpChannelConfig extends DefaultChannelConfig {

    private int protocolId = 12345;

    public RudpChannelConfig(Channel channel) {
        super(channel);
    }

    public int getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(int protocolId) {
        if (protocolId <= 0) {
            throw new IllegalArgumentException("protocolId > 0");
        }

        this.protocolId = protocolId;
    }
}
