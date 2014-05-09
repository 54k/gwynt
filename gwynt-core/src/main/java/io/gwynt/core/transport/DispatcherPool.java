package io.gwynt.core.transport;

public interface DispatcherPool {

    Dispatcher getDispatcher();

    void start();

    void stop();
}
