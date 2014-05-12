package io.gwynt.example;

import io.gwynt.core.AbstractIoHandler;
import io.gwynt.core.Endpoint;
import io.gwynt.core.TcpEndpoint;
import io.gwynt.core.UdpEndpoint;
import io.gwynt.core.pipeline.IoHandlerContext;
import io.gwynt.core.transport.tcp.NioTcpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Date;

public class Main {

    public static void main(String[] args) throws Exception {
        StringConverter sc = new StringConverter();
        MainHandler mh = new MainHandler();
        Endpoint tcpEndpoint = new TcpEndpoint().addHandler(sc).addHandler(mh).bind(3000);
        new UdpEndpoint().setScheduler(tcpEndpoint.getScheduler()).addHandler(sc).addHandler(mh).bind(3001);
    }

    private static class StringConverter extends AbstractIoHandler<byte[], String> {

        private Charset charset = Charset.forName("UTF-8");

        @Override
        public void onMessageReceived(IoHandlerContext context, byte[] message) {
            ByteBuffer buffer = ByteBuffer.wrap(message);
            CharBuffer charBuffer = charset.decode(buffer);
            buffer.clear();
            context.fireMessageReceived(charBuffer.toString());
        }

        @Override
        public void onMessageSent(IoHandlerContext context, String message) {
            ByteBuffer buffer = charset.encode(message);
            byte[] messageBytes = new byte[buffer.limit()];
            buffer.get(messageBytes);
            buffer.clear();
            context.fireMessageSent(messageBytes);
        }
    }

    private static class MainHandler extends AbstractIoHandler<String, Object> {

        private static final Logger logger = LoggerFactory.getLogger(MainHandler.class);

        @Override
        public void onMessageReceived(IoHandlerContext context, String message) {
            context.fireMessageSent("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
            context.fireMessageSent(new Date().toString() + "\r\n");
            if (context.getIoSession() instanceof NioTcpSession) {
                context.fireClosing();
            }
        }

        @Override
        public void onExceptionCaught(IoHandlerContext context, Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }
}
