package io.gwynt.core.nio;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelException;
import io.gwynt.core.DefaultChannelConfig;
import io.gwynt.core.buffer.FixedRecvByteBufferAllocator;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;

public class NioDatagramChannelConfig extends DefaultChannelConfig {

    private DatagramChannel ch;

    public NioDatagramChannelConfig(Channel channel, DatagramChannel ch) {
        super(channel);
        this.ch = ch;
        setRecvByteBufferAllocator(FixedRecvByteBufferAllocator.DEFAULT);
        try {
            ch.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        } catch (IOException e) {
            throw new ChannelException(e);
        }
    }
}
