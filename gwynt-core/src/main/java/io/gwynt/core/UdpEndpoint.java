package io.gwynt.core;

import io.gwynt.core.scheduler.SingleThreadedEventScheduler;
import io.gwynt.core.transport.NioDispatcher;
import io.gwynt.core.transport.udp.NioUdpSessionFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.DatagramChannel;

public class UdpEndpoint extends AbstractEndpoint {

    private NioDispatcher dispatcher;

    public UdpEndpoint() {
        ioSessionFactory = new NioUdpSessionFactory(this);
        eventScheduler = new SingleThreadedEventScheduler();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Endpoint bind(int port) {
        try {
            eventScheduler.start();

            DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
            channel.configureBlocking(false);
            channel.socket().setSoTimeout(500);
            channel.socket().setReuseAddress(true);
            channel.bind(new InetSocketAddress(port));
            initialize(channel);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private void initialize(DatagramChannel channel) throws IOException {
        dispatcher = new NioDispatcher(ioSessionFactory);
        dispatcher.setName("gwynt-upd-dispatcher");
        dispatcher.start();
        dispatcher.register(channel);
    }

    @Override
    public Endpoint unbind() {
        dispatcher.stop();
        eventScheduler.stop();
        return this;
    }
}
