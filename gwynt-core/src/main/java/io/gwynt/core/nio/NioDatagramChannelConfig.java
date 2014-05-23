package io.gwynt.core.nio;

import io.gwynt.core.Channel;
import io.gwynt.core.DefaultChannelConfig;
import io.gwynt.core.FixedRecvByteBufferAllocator;

public class NioDatagramChannelConfig extends DefaultChannelConfig {

    public NioDatagramChannelConfig(Channel channel) {
        super(channel);
        setRecvByteBufferAllocator(FixedRecvByteBufferAllocator.DEFAULT);
    }
}
