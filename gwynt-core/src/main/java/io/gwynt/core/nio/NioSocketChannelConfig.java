package io.gwynt.core.nio;

import io.gwynt.core.Channel;
import io.gwynt.core.DefaultChannelConfig;

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
            throw new RuntimeException(e);
        }
    }
}
