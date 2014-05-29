package io.gwynt.core.group;

import io.gwynt.core.Channel;

public interface ChannelMatcher {

    boolean match(Channel channel);
}
