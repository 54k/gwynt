package io.gwynt.core.oio;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelOption;
import io.gwynt.core.DefaultChannelConfig;

import java.net.MulticastSocket;
import java.net.NetworkInterface;

public class OioDatagramChannelConfig extends DefaultChannelConfig {

    OioDatagramChannelConfig(Channel channel) {
        super(channel);
    }

    @Override
    protected MulticastSocket javaChannel() {
        return (MulticastSocket) super.javaChannel();
    }

    @Override
    protected <T> boolean setOption0(ChannelOption<T> channelOption, T value) throws Exception {
        boolean isSet = true;

        if (channelOption == ChannelOption.IP_MULTICAST_IF) {
            javaChannel().setNetworkInterface((NetworkInterface) value);
        } else if (channelOption == ChannelOption.IP_MULTICAST_LOOP) {
            javaChannel().setLoopbackMode((Boolean) value);
        } else if (channelOption == ChannelOption.IP_MULTICAST_TTL) {
            javaChannel().setTimeToLive((Integer) value);
        } else if (channelOption == ChannelOption.SO_BROADCAST) {
            javaChannel().setBroadcast((Boolean) value);
        } else if (channelOption == ChannelOption.SO_REUSEADDR) {
            javaChannel().setReuseAddress((Boolean) value);
        } else if (channelOption == ChannelOption.SO_RCVBUF) {
            javaChannel().setReceiveBufferSize((Integer) value);
        } else if (channelOption == ChannelOption.SO_SNDBUF) {
            javaChannel().setSendBufferSize((Integer) value);
        } else {
            isSet = false;
        }

        return isSet;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T getOption0(ChannelOption<T> channelOption) throws Exception {
        Object result = null;

        if (channelOption == ChannelOption.IP_MULTICAST_IF) {
            result = javaChannel().getNetworkInterface();
        } else if (channelOption == ChannelOption.IP_MULTICAST_LOOP) {
            result = javaChannel().getLoopbackMode();
        } else if (channelOption == ChannelOption.IP_MULTICAST_TTL) {
            result = javaChannel().getTimeToLive();
        } else if (channelOption == ChannelOption.SO_BROADCAST) {
            result = javaChannel().getBroadcast();
        } else if (channelOption == ChannelOption.SO_REUSEADDR) {
            result = javaChannel().getReuseAddress();
        } else if (channelOption == ChannelOption.SO_RCVBUF) {
            result = javaChannel().getReceiveBufferSize();
        } else if (channelOption == ChannelOption.SO_SNDBUF) {
            result = javaChannel().getSendBufferSize();
        }

        return (T) result;
    }
}
