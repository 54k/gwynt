package io.gwynt.core.transport;

import io.gwynt.core.IoSessionFactory;
import io.gwynt.core.transport.tcp.NioTcpSession;

import java.nio.channels.SelectableChannel;

public class NioDispatcherPool implements DispatcherPool {

    private int dispatchersCount = Math.max(1, (Runtime.getRuntime().availableProcessors() - 1) * 2);
    private Dispatcher[] dispatchers = new NioDispatcher[dispatchersCount];
    private int currentDispatcher = 0;

    private IoSessionFactory<SelectableChannel, NioTcpSession> ioSessionFactory;

    public NioDispatcherPool(IoSessionFactory<SelectableChannel, NioTcpSession> ioSessionFactory) {
        this.ioSessionFactory = ioSessionFactory;
        createDispatchers();
    }

    private void createDispatchers() {
        for (int i = 0; i < dispatchersCount; i++) {
            NioDispatcher dispatcher = new NioDispatcher(ioSessionFactory);
            dispatcher.setName("gwynt-tcp-dispatcher-" + (i + 1));
            dispatchers[i] = dispatcher;
        }
    }

    @Override
    public Dispatcher getDispatcher() {
        currentDispatcher = currentDispatcher % dispatchers.length;
        Dispatcher dispatcher = dispatchers[currentDispatcher];
        currentDispatcher++;
        return dispatcher;
    }

    @Override
    public void start() {
        for (Dispatcher dispatcher : dispatchers) {
            dispatcher.start();
        }
        currentDispatcher = 0;
    }

    @Override
    public void stop() {
        for (Dispatcher dispatcher : dispatchers) {
            dispatcher.stop();
        }
    }
}
