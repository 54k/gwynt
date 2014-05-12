package io.gwynt.core.transport.tcp;

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
        return new NioTcpSession(new NioSocketChannel(channel), endpoint);
    }

}
