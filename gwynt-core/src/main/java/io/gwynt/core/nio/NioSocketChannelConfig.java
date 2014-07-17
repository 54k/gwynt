package io.gwynt.core.nio;

import io.gwynt.core.ChannelException;
import io.gwynt.core.ChannelOption;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public final class NioSocketChannelConfig extends NetworkChannelConfig {

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

    @Override
    protected <T> boolean setOption0(ChannelOption<T> channelOption, T value) throws Exception {
        try {
            if (channelOption == ChannelOption.TRAFFIC_CLASS) {
                javaChannel().socket().setTrafficClass((Integer) value);
            } else if (channelOption == ChannelOption.SO_TIMEOUT) {
                javaChannel().socket().setSoTimeout((Integer) value);
            } else {
                return false;
            }

            return true;
        } catch (IOException e) {
            throw new ChannelException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T getOption0(ChannelOption<T> channelOption) throws Exception {
        try {
            Object result = null;
            if (channelOption == ChannelOption.TRAFFIC_CLASS) {
                result = javaChannel().socket().getTrafficClass();
            } else if (channelOption == ChannelOption.SO_TIMEOUT) {
                result = javaChannel().socket().getSoTimeout();
            }

            return (T) result;
        } catch (IOException e) {
            throw new ChannelException(e);
        }
    }
}
