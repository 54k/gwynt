package io.gwynt.core.transport;

import io.gwynt.core.Channel;
import io.gwynt.core.Endpoint;
import io.gwynt.core.IoSessionFactory;

import java.nio.channels.SocketChannel;

public class NioSessionFactory implements IoSessionFactory<SocketChannel, AbstractNioSession> {

    private Endpoint endpoint;

    public NioSessionFactory(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public AbstractNioSession createConnection(SocketChannel channel) {
        return new SessionImpl(new NioSocketChannel(channel), endpoint);
    }

    @SuppressWarnings("unchecked")
    private static class SessionImpl extends AbstractNioSession {
        private SessionImpl(Channel channel, Endpoint endpoint) {
            super(channel, endpoint);
        }
    }
}
