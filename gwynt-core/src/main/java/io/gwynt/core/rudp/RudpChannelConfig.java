package io.gwynt.core.rudp;

import io.gwynt.core.Channel;
import io.gwynt.core.DefaultChannelConfig;

public class RudpChannelConfig extends DefaultChannelConfig {

    private int protocolId = 1337;
    private int disconnectTimeoutMillis = 0;

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

    public int getDisconnectTimeoutMillis() {
        return disconnectTimeoutMillis;
    }

    public void setDisconnectTimeoutMillis(int disconnectTimeoutMillis) {
        if (disconnectTimeoutMillis < 0) {
            throw new IllegalArgumentException("disconnectTimeoutMillis >= 0");
        }
        this.disconnectTimeoutMillis = disconnectTimeoutMillis;
    }
}
