package io.gwynt.core;

public interface ChannelFactory<I extends Channel> {

    I createChannel(Class<? extends I> channelClass);
}
