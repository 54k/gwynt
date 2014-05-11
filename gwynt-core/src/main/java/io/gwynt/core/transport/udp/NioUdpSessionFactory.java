package io.gwynt.core.transport.udp;

import io.gwynt.core.Channel;
import io.gwynt.core.Endpoint;
import io.gwynt.core.IoSessionFactory;
import io.gwynt.core.transport.tcp.NioTcpSession;

import java.nio.channels.DatagramChannel;

public class NioUdpSessionFactory implements IoSessionFactory<DatagramChannel, NioTcpSession> {

    private Endpoint endpoint;

    public NioUdpSessionFactory(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public NioTcpSession createConnection(DatagramChannel channel) {
        return new SessionImpl(new NioDatagramChannel(channel), endpoint);
    }

    @SuppressWarnings("unchecked")
    private static class SessionImpl extends NioUpdSession {
        private SessionImpl(Channel channel, Endpoint endpoint) {
            super(channel, endpoint);
        }
    }
}
