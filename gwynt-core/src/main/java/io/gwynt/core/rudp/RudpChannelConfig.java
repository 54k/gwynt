package io.gwynt.core.rudp;

import io.gwynt.core.Channel;
import io.gwynt.core.DefaultChannelConfig;

public class RudpChannelConfig extends DefaultChannelConfig {

    private int protocolMagic = 1337;
    private int disconnectTimeoutMillis = 1000;

    public RudpChannelConfig(Channel channel) {
        super(channel);
    }

    public int getProtocolMagic() {
        return protocolMagic;
    }

    public void setProtocolMagic(int protocolMagic) {
        if (protocolMagic <= 0) {
            throw new IllegalArgumentException("protocolMagic > 0");
        }
        this.protocolMagic = protocolMagic;
    }

    public int getDisconnectTimeoutMillis() {
        return disconnectTimeoutMillis;
    }

    public void setDisconnectTimeoutMillis(int disconnectTimeoutMillis) {
        if (disconnectTimeoutMillis <= 0) {
            throw new IllegalArgumentException("disconnectTimeoutMillis > 0");
        }
        this.disconnectTimeoutMillis = disconnectTimeoutMillis;
    }
}
