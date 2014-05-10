package io.gwynt.core;

import io.gwynt.core.scheduler.SingleThreadedEventScheduler;
import io.gwynt.core.transport.DispatcherPool;
import io.gwynt.core.transport.NioAcceptor;
import io.gwynt.core.transport.NioDispatcherPool;
import io.gwynt.core.transport.NioSessionFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class TcpEndpoint extends AbstractEndpoint {

    private DispatcherPool dispatcherPool;
    private NioAcceptor acceptor;

    public TcpEndpoint() {
        ioSessionFactory = new NioSessionFactory(this);
        eventScheduler = new SingleThreadedEventScheduler();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Endpoint bind(int port) {
        try {
            ServerSocketChannel channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            channel.socket().setSoTimeout(500);
            channel.socket().setReuseAddress(true);
            channel.socket().setPerformancePreferences(0, 1, 2);
            channel.bind(new InetSocketAddress(port));
            initialize(channel);
            eventScheduler.start();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private void initialize(ServerSocketChannel channel) throws IOException {
        DispatcherPool dispatcherPool = new NioDispatcherPool(ioSessionFactory);
        this.dispatcherPool = dispatcherPool;

        NioAcceptor acceptor = new NioAcceptor(dispatcherPool);
        acceptor.setName("gwynt-acceptor");
        acceptor.start();
        this.acceptor = acceptor;
        dispatcherPool.start();
        acceptor.register(channel);
    }

    @Override
    public Endpoint unbind() {
        acceptor.stop();
        dispatcherPool.stop();
        eventScheduler.stop();
        return this;
    }
}
