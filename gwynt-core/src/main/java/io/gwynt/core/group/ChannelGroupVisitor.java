package io.gwynt.core.group;

public interface ChannelGroupVisitor<T> {

    T visit(ChannelGroup channelGroup);
}
