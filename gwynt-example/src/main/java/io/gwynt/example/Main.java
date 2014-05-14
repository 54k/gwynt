package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Endpoint;
import io.gwynt.core.EndpointBootstrap;
import io.gwynt.core.pipeline.HandlerContext;
import io.gwynt.core.transport.Datagram;
import io.gwynt.core.transport.NioDatagramChannel;
import io.gwynt.core.transport.NioServerSocketChannel;
import io.gwynt.core.transport.NioSocketChannel;
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
        LoggingHandler lh = new LoggingHandler();
        EchoHandler eh = new EchoHandler();

        Endpoint tcpEndpoint = new EndpointBootstrap().setChannel(NioServerSocketChannel.class).addHandler(sc).addHandler(lh).addHandler(eh).bind(3000);

        new EndpointBootstrap().setChannel(NioSocketChannel.class).setScheduler(tcpEndpoint.getScheduler()).addHandler(sc).addHandler(lh)
                .addHandler(new AbstractHandler<String, String>() {
                    private Logger logger = LoggerFactory.getLogger(getClass());

                    @Override
                    public void onOpen(HandlerContext context) {
                        context.fireMessageSent("echo echo echo");
                    }

                    @Override
                    public void onMessageReceived(HandlerContext context, String message) {
                        context.fireMessageSent(message);
                        context.fireClosing();
                    }

                    @Override
                    public void onExceptionCaught(HandlerContext context, Throwable e) {
                        logger.error(e.getMessage(), e);
                    }
                }).connect("localhost", 3000);

        new EndpointBootstrap().setChannel(NioServerSocketChannel.class).setScheduler(tcpEndpoint.getScheduler()).addHandler(sc).addHandler(lh).addHandler(mh).bind(3001);

        new EndpointBootstrap().setChannel(NioDatagramChannel.class).setScheduler(tcpEndpoint.getScheduler()).addHandler(lh).addHandler(new AbstractHandler() {
            @Override
            public void onMessageReceived(HandlerContext context, Object message) {
                context.fireMessageSent(message);
            }
        }).bind(3001);

        new EndpointBootstrap().setChannel(NioDatagramChannel.class).setScheduler(tcpEndpoint.getScheduler()).addHandler(lh).addHandler(new AbstractHandler() {
            @Override
            public void onOpen(HandlerContext context) {
                context.fireMessageSent(new Datagram(context.getChannel().getRemoteAddress(), ByteBuffer.wrap("datagram".getBytes())));
            }

            @Override
            public void onMessageReceived(HandlerContext context, Object message) {
                context.fireMessageSent(message);
                context.fireClosing();
            }
        }).connect("localhost", 3001);
    }

    private static class StringConverter extends AbstractHandler<byte[], String> {

        private Charset charset = Charset.forName("UTF-8");

        @Override
        public void onMessageReceived(HandlerContext context, byte[] message) {
            ByteBuffer buffer = ByteBuffer.wrap(message);
            CharBuffer charBuffer = charset.decode(buffer);
            buffer.clear();
            context.fireMessageReceived(charBuffer.toString());
        }

        @Override
        public void onMessageSent(HandlerContext context, String message) {
            ByteBuffer buffer = charset.encode(message);
            byte[] messageBytes = new byte[buffer.limit()];
            buffer.get(messageBytes);
            buffer.clear();
            context.fireMessageSent(messageBytes);
        }
    }

    private static class MainHandler extends AbstractHandler<String, Object> {

        private static final Logger logger = LoggerFactory.getLogger(MainHandler.class);

        @Override
        public void onMessageReceived(HandlerContext context, String message) {
            context.fireMessageSent("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
            context.fireMessageSent(new Date().toString() + "\r\n");
            if (context.getChannel() instanceof NioSocketChannel) {
                context.fireClosing();
            }
        }

        @Override
        public void onExceptionCaught(HandlerContext context, Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static class EchoHandler extends AbstractHandler<String, Object> {

        @Override
        public void onMessageReceived(HandlerContext context, String message) {
            context.fireMessageSent(message);
        }
    }

    private static class LoggingHandler extends AbstractHandler {

        private static final Logger logger = LoggerFactory.getLogger(LoggingHandler.class);

        @Override
        public void onExceptionCaught(HandlerContext context, Throwable e) {
            logger.info("Exception caught on channel {}: {} ", context.getChannel(), e);
            logger.error(e.getMessage(), e);
            context.fireExceptionCaught(e);
        }

        @Override
        public void onClose(HandlerContext context) {
            logger.info("Channel closed: " + context.getChannel());
            context.fireClose();
        }

        @Override
        public void onClosing(HandlerContext context) {
            logger.info("Channel closing: " + context.getChannel());
            context.fireClosing();
        }

        @Override
        public void onMessageSent(HandlerContext context, Object message) {
            logger.info("Message sent: " + message);
            context.fireMessageSent(message);
        }

        @Override
        public void onMessageReceived(HandlerContext context, Object message) {
            logger.info("Message received: {}", message);
            context.fireMessageReceived(message);
        }

        @Override
        public void onOpen(HandlerContext context) {
            logger.info("Channel opened: {}", context.getChannel());
            context.fireOpen();
        }

        @Override
        public void onUnregistered(HandlerContext context) {
            logger.info("Channel unregistered: {}", context.getChannel());
            context.fireUnregistered();
        }

        @Override
        public void onRegistered(HandlerContext context) {
            logger.info("Channel registered: {}", context.getChannel());
            context.fireRegistered();
        }
    }
}
