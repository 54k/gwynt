package io.gwynt.core;

public interface ChannelFactory<I, O extends Channel> {

    O createChannel(I channel);
}
