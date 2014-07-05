package io.gwynt.core.rudp;

import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.MulticastChannel;

public interface RudpChannel extends MulticastChannel {

    ChannelFuture writeReliable(Object message);

    ChannelFuture writeReliable(Object message, ChannelPromise channelPromise);
}
