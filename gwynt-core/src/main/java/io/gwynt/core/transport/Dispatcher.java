package io.gwynt.core.transport;

import io.gwynt.core.Channel;

public interface Dispatcher {

    Dispatcher next();

    void register(Channel channel);

    void unregister(Channel channel);

    void modifyRegistration(Channel channel, int interestOps);
}
