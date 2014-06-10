package io.gwynt.example;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IoSession;
import org.apache.mina.transport.nio.NioTcpServer;

import java.nio.ByteBuffer;
import java.util.Date;

public class MinaSimpleServer implements Runnable {

    @Override
    public void run() {
        NioTcpServer acceptor = new NioTcpServer();
        acceptor.setIoHandler(new AbstractIoHandler() {
            @Override
            public void messageReceived(IoSession session, Object message) {
                session.write(ByteBuffer.wrap("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n".getBytes()));
                session.write(ByteBuffer.wrap((new Date().toString() + "\r\n").getBytes()));
                session.close(false);
            }
        });
        acceptor.bind(3002);
    }
}
