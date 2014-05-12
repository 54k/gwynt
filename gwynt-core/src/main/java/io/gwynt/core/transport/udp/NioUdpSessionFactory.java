package io.gwynt.core.transport.udp;

import io.gwynt.core.Endpoint;
import io.gwynt.core.IoSessionFactory;

import java.nio.channels.DatagramChannel;

public class NioUdpSessionFactory implements IoSessionFactory<DatagramChannel, NioUpdSession> {

    private Endpoint endpoint;

    public NioUdpSessionFactory(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public NioUpdSession createConnection(DatagramChannel channel) {
        return new NioUpdSession(new NioDatagramChannel(channel), endpoint);
    }

}
