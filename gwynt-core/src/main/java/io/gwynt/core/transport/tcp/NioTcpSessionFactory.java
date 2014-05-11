package io.gwynt.core.transport.tcp;

import io.gwynt.core.Channel;
import io.gwynt.core.Endpoint;
import io.gwynt.core.IoSessionFactory;

import java.nio.channels.SocketChannel;

public class NioTcpSessionFactory implements IoSessionFactory<SocketChannel, NioTcpSession> {

    private Endpoint endpoint;

    public NioTcpSessionFactory(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public NioTcpSession createConnection(SocketChannel channel) {
        return new TcpSessionImpl(new NioSocketChannel(channel), endpoint);
    }

    @SuppressWarnings("unchecked")
    private static class TcpSessionImpl extends NioTcpSession {
        private TcpSessionImpl(Channel channel, Endpoint endpoint) {
            super(channel, endpoint);
        }
    }
}
