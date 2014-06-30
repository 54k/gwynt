package io.gwynt.core.oio;

import io.gwynt.core.ThreadPerChannelEventLoopGroup;

public class OioEventLoopGroup extends ThreadPerChannelEventLoopGroup {

    public OioEventLoopGroup(int maxChannels) {
        super(maxChannels);
    }
}
