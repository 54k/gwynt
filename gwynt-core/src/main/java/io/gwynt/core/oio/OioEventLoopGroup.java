package io.gwynt.core.oio;

import io.gwynt.core.ThreadPerChannelEventLoopGroup;
import io.gwynt.core.concurrent.DefaultThreadFactory;

public class OioEventLoopGroup extends ThreadPerChannelEventLoopGroup {

    public OioEventLoopGroup() {
        this(0);
    }

    public OioEventLoopGroup(int maxChannels) {
        super(maxChannels, new DefaultThreadFactory("gwynt-oio-eventloop-"));
    }
}
