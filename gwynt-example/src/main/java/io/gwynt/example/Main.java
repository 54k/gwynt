package io.gwynt.example;

import io.gwynt.core.AbstractIoHandler;
import io.gwynt.core.Endpoint;
import io.gwynt.core.TcpEndpoint;
import io.gwynt.core.UdpEndpoint;
import io.gwynt.core.pipeline.IoHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class Main {

    public static void main(String[] args) throws Exception {
        StringConverter sc = new StringConverter();
        MainHandler mh = new MainHandler();
        Endpoint tcpEndpoint = new TcpEndpoint().addHandler(sc).addHandler(mh).bind(3000);
        new UdpEndpoint().setScheduler(tcpEndpoint.getScheduler()).addHandler(sc).addHandler(mh).bind(3001);
    }

    private static class StringConverter extends AbstractIoHandler<byte[], String> {

        @Override
        public void onMessageReceived(IoHandlerContext context, byte[] message) {
            context.fireMessageReceived(new String(message));
        }

        @Override
        public void onMessageSent(IoHandlerContext context, String message) {
            context.fireMessageSent(message.getBytes());
        }
    }

    private static class MainHandler extends AbstractIoHandler<String, Object> {
        private static final Logger logger = LoggerFactory.getLogger(MainHandler.class);

        @Override
        public void onMessageReceived(IoHandlerContext context, String message) {
            context.fireMessageSent("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
            context.fireMessageSent(new Date().toString() + "\r\n");
            context.fireClosing();
        }

        @Override
        public void onExceptionCaught(IoHandlerContext context, Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }
}
