package io.gwynt.core.transport;

import java.nio.channels.SelectableChannel;

public interface Dispatcher {

    void register(SelectableChannel channel);

    void unregister(SelectableChannel channel);

    void modifyRegistration(SelectableChannel channel, int interestOps);

    void start();

    void stop();
}
