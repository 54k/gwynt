package io.gwynt.core;

import io.gwynt.core.scheduler.SingleThreadedEventScheduler;
import io.gwynt.core.transport.DispatcherPool;
import io.gwynt.core.transport.NioEventLoop;
import io.gwynt.core.transport.NioServerSocketChannel;
import io.gwynt.core.transport.NioSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;

public class TcpConnector extends AbstractEndpoint {

    private DispatcherPool dispatcherPool;
    //    private Acceptor acceptor;

    public TcpConnector() {
        //        ioSessionFactory = new NioTcpSessionFactory(this);
        eventScheduler = new SingleThreadedEventScheduler();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Endpoint bind(int port) {
        try {
            eventScheduler.start();

            //            ServerSocketChannel channel = ServerSocketChannel.open();
            //            channel.configureBlocking(false);
            //            channel.socket().setSoTimeout(500);
            //            channel.socket().setReuseAddress(true);
            //            channel.socket().setPerformancePreferences(0, 1, 2);
            //            channel.bind(new InetSocketAddress(port));
            NioSocketChannel channel = new NioSocketChannel(this);
            initialize(channel);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private void initialize(NioSocketChannel channel) throws IOException {

        NioEventLoop eventLoop = new NioEventLoop();
        eventLoop.runThread();

        eventLoop.register(channel);
        //        DispatcherPool dispatcherPool = new NioDispatcherPool(ioSessionFactory);
        //        this.dispatcherPool = dispatcherPool;
        //
        //        Acceptor acceptor = new Acceptor(dispatcherPool);
        //        acceptor.setName("gwynt-tcp-acceptor");
        //        acceptor.runThread();
        //        this.acceptor = acceptor;
        //        dispatcherPool.start();
        //        acceptor.register(unsafe);
    }

    @Override
    public Endpoint unbind() {
        //        acceptor.shutdownThread();
        //        dispatcherPool.stop();
        eventScheduler.stop();
        return this;
    }
}
