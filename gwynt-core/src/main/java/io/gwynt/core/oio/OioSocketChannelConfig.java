package io.gwynt.core.oio;

import io.gwynt.core.ChannelOption;
import io.gwynt.core.DefaultChannelConfig;

import java.net.Socket;

public class OioSocketChannelConfig extends DefaultChannelConfig {

    OioSocketChannelConfig(OioSocketChannel channel) {
        super(channel);
    }

    @Override
    protected Socket javaChannel() {
        return (Socket) super.javaChannel();
    }

    @Override
    protected <T> boolean setOption0(ChannelOption<T> channelOption, T value) throws Exception {
        boolean isSet = true;
        if (channelOption == ChannelOption.SO_LINGER) {
            javaChannel().setSoLinger(true, (Integer) value);
        } else if (channelOption == ChannelOption.SO_REUSEADDR) {
            javaChannel().setReuseAddress((Boolean) value);
        } else if (channelOption == ChannelOption.SO_RCVBUF) {
            javaChannel().setReceiveBufferSize((Integer) value);
        } else if (channelOption == ChannelOption.SO_SNDBUF) {
            javaChannel().setSendBufferSize((Integer) value);
        } else if (channelOption == ChannelOption.SO_KEEPALIVE) {
            javaChannel().setKeepAlive((Boolean) value);
        } else if (channelOption == ChannelOption.TCP_NODELAY) {
            javaChannel().setTcpNoDelay((Boolean) value);
        } else if (channelOption == ChannelOption.TRAFFIC_CLASS) {
            javaChannel().setTrafficClass((Integer) value);
        } else {
            isSet = false;
        }

        return isSet;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T getOption0(ChannelOption<T> channelOption) throws Exception {
        Object result = null;
        if (channelOption == ChannelOption.SO_LINGER) {
            result = javaChannel().getSoLinger();
        } else if (channelOption == ChannelOption.SO_REUSEADDR) {
            result = javaChannel().getReuseAddress();
        } else if (channelOption == ChannelOption.SO_RCVBUF) {
            result = javaChannel().getReceiveBufferSize();
        } else if (channelOption == ChannelOption.SO_SNDBUF) {
            result = javaChannel().getSendBufferSize();
        } else if (channelOption == ChannelOption.SO_KEEPALIVE) {
            result = javaChannel().getKeepAlive();
        } else if (channelOption == ChannelOption.TCP_NODELAY) {
            result = javaChannel().getTcpNoDelay();
        } else if (channelOption == ChannelOption.TRAFFIC_CLASS) {
            result = javaChannel().getTrafficClass();
        }

        return (T) result;
    }
}
