package io.gwynt.core;

import io.gwynt.core.scheduler.SingleThreadedEventScheduler;
import io.gwynt.core.transport.NioDatagramChannel;
import io.gwynt.core.transport.NioEventLoop;
import io.gwynt.core.transport.NioServerSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

public class UdpEndpoint extends AbstractEndpoint {

    //    private NioNioEventLoop dispatcher;

    public UdpEndpoint() {
        //        ioSessionFactory = new NioUdpSessionFactory(this);
        eventScheduler = new SingleThreadedEventScheduler();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Endpoint bind(int port) {
        try {
            eventScheduler.start();
            NioDatagramChannel channel = new NioDatagramChannel(this);
            channel.unsafe().bind(new InetSocketAddress(port));
            initialize(channel);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private void initialize(NioDatagramChannel channel) throws IOException {
        NioEventLoop eventLoop = new NioEventLoop();
        eventLoop.runThread();

        eventLoop.register(channel);
    }

    @Override
    public Endpoint unbind() {
        //        dispatcher.shutdownThread();
        eventScheduler.stop();
        return this;
    }
}
