package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelCallback;

public interface Dispatcher {

    Dispatcher next();

    void register(Channel channel);

    void register(Channel channel, ChannelCallback callback);

    void unregister(Channel channel);

    void unregister(Channel channel, ChannelCallback callback);

    void modifyRegistration(Channel channel, int interestOps);

    void modifyRegistration(Channel channel, int interestOps, ChannelCallback callback);
}
