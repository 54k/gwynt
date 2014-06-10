package io.gwynt.core.nio;

import io.gwynt.core.Channel;
import io.gwynt.core.DefaultChannelConfig;
import io.gwynt.core.exception.ChannelException;

import java.net.SocketException;
import java.nio.channels.SocketChannel;

public class NioSocketChannelConfig extends DefaultChannelConfig {

    private SocketChannel ch;

    public NioSocketChannelConfig(Channel channel, SocketChannel ch) {
        super(channel);
        this.ch = ch;

        try {
            ch.socket().setTcpNoDelay(true);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    public int getTrafficClass() {
        try {
            return ch.socket().getTrafficClass();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    public NioSocketChannelConfig setTrafficClass(int tc) {
        try {
            ch.socket().setTrafficClass(tc);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    public NioSocketChannelConfig setPerfomancePreferences(int connectionTime, int latency, int bandwidth) {
        ch.socket().setPerformancePreferences(connectionTime, latency, bandwidth);
        return this;
    }

    public boolean getReuseAddress() {
        try {
            return ch.socket().getReuseAddress();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    public NioSocketChannelConfig setReuseAddress(boolean on) {
        try {
            ch.socket().setReuseAddress(on);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }
}
