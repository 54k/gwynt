package io.gwynt.core.nio;

import io.gwynt.core.ChannelException;
import io.gwynt.core.ChannelOption;
import io.gwynt.core.DefaultChannelConfig;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.nio.channels.NetworkChannel;

abstract class NetworkChannelConfig extends DefaultChannelConfig {

    protected NetworkChannelConfig(AbstractNioChannel channel) {
        super(channel);
    }

    @Override
    protected NetworkChannel javaChannel() {
        return (NetworkChannel) super.javaChannel();
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> channelOption, T value) {
        try {
            boolean isSet = true;
            if (channelOption == ChannelOption.IP_MULTICAST_IF) {
                javaChannel().setOption(StandardSocketOptions.IP_MULTICAST_IF, (NetworkInterface) value);
            } else if (channelOption == ChannelOption.IP_MULTICAST_LOOP) {
                javaChannel().setOption(StandardSocketOptions.IP_MULTICAST_LOOP, (Boolean) value);
            } else if (channelOption == ChannelOption.IP_MULTICAST_TTL) {
                javaChannel().setOption(StandardSocketOptions.IP_MULTICAST_TTL, (Integer) value);
            } else if (channelOption == ChannelOption.IP_TOS) {
                javaChannel().setOption(StandardSocketOptions.IP_TOS, (Integer) value);
            } else if (channelOption == ChannelOption.SO_BROADCAST) {
                javaChannel().setOption(StandardSocketOptions.SO_BROADCAST, (Boolean) value);
            } else if (channelOption == ChannelOption.SO_LINGER) {
                javaChannel().setOption(StandardSocketOptions.SO_LINGER, (Integer) value);
            } else if (channelOption == ChannelOption.SO_REUSEADDR) {
                javaChannel().setOption(StandardSocketOptions.SO_REUSEADDR, (Boolean) value);
            } else if (channelOption == ChannelOption.SO_RCVBUF) {
                javaChannel().setOption(StandardSocketOptions.SO_RCVBUF, (Integer) value);
            } else if (channelOption == ChannelOption.SO_SNDBUF) {
                javaChannel().setOption(StandardSocketOptions.SO_SNDBUF, (Integer) value);
            } else if (channelOption == ChannelOption.SO_KEEPALIVE) {
                javaChannel().setOption(StandardSocketOptions.SO_KEEPALIVE, (Boolean) value);
            } else if (channelOption == ChannelOption.TCP_NODELAY) {
                javaChannel().setOption(StandardSocketOptions.TCP_NODELAY, (Boolean) value);
            } else {
                isSet = false;
            }

            return super.setOption(channelOption, value) || isSet;
        } catch (IOException e) {
            throw new ChannelException(e);
        } catch (UnsupportedOperationException ignore) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOption(ChannelOption<T> channelOption) {
        try {
            Object result = null;
            if (channelOption == ChannelOption.IP_MULTICAST_IF) {
                result = javaChannel().getOption(StandardSocketOptions.IP_MULTICAST_IF);
            } else if (channelOption == ChannelOption.IP_MULTICAST_LOOP) {
                result = javaChannel().getOption(StandardSocketOptions.IP_MULTICAST_LOOP);
            } else if (channelOption == ChannelOption.IP_MULTICAST_TTL) {
                result = javaChannel().getOption(StandardSocketOptions.IP_MULTICAST_TTL);
            } else if (channelOption == ChannelOption.IP_TOS) {
                result = javaChannel().getOption(StandardSocketOptions.IP_TOS);
            } else if (channelOption == ChannelOption.SO_BROADCAST) {
                result = javaChannel().getOption(StandardSocketOptions.SO_BROADCAST);
            } else if (channelOption == ChannelOption.SO_LINGER) {
                result = javaChannel().getOption(StandardSocketOptions.SO_LINGER);
            } else if (channelOption == ChannelOption.SO_REUSEADDR) {
                result = javaChannel().getOption(StandardSocketOptions.SO_REUSEADDR);
            } else if (channelOption == ChannelOption.SO_RCVBUF) {
                result = javaChannel().getOption(StandardSocketOptions.SO_RCVBUF);
            } else if (channelOption == ChannelOption.SO_SNDBUF) {
                result = javaChannel().getOption(StandardSocketOptions.SO_SNDBUF);
            } else if (channelOption == ChannelOption.SO_KEEPALIVE) {
                result = javaChannel().getOption(StandardSocketOptions.SO_KEEPALIVE);
            } else if (channelOption == ChannelOption.TCP_NODELAY) {
                result = javaChannel().getOption(StandardSocketOptions.TCP_NODELAY);
            }

            if (result == null) {
                return super.getOption(channelOption);
            }
            return (T) result;
        } catch (IOException e) {
            throw new ChannelException(e);
        } catch (UnsupportedOperationException ignore) {
            return null;
        }
    }
}
