package io.gwynt.core.nio;

import java.nio.channels.SocketChannel;

public class NioSocketChannelConfig extends NetworkChannelConfig {

    NioSocketChannelConfig(NioSocketChannel channel) {
        super(channel);
    }

    @Override
    protected SocketChannel javaChannel() {
        return (SocketChannel) super.javaChannel();
    }

    public NioSocketChannelConfig setPerfomancePreferences(int connectionTime, int latency, int bandwidth) {
        javaChannel().socket().setPerformancePreferences(connectionTime, latency, bandwidth);
        return this;
    }
}
