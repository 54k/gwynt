package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelListener;
import io.gwynt.core.Endpoint;
import io.gwynt.core.EndpointBootstrap;
import io.gwynt.core.pipeline.HandlerContext;
import io.gwynt.core.transport.Datagram;
import io.gwynt.core.transport.NioDatagramChannel;
import io.gwynt.core.transport.NioEventLoopGroup;
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

        NioEventLoopGroup dispatcher = new NioEventLoopGroup();
        dispatcher.runThread();

        Endpoint tcpEndpoint =
                new EndpointBootstrap().setDispatcher(dispatcher).setChannel(NioServerSocketChannel.class).addHandler(sc).addHandler(lh).addHandler(eh).bind(3000);

        new EndpointBootstrap().setDispatcher(dispatcher).setChannel(NioSocketChannel.class).setScheduler(tcpEndpoint.getScheduler()).addHandler(sc).addHandler(lh)
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

        new EndpointBootstrap().setDispatcher(dispatcher).setChannel(NioServerSocketChannel.class).setScheduler(tcpEndpoint.getScheduler()).addHandler(sc).addHandler(lh)
                .addHandler(mh).bind(3001);

        new EndpointBootstrap().setDispatcher(dispatcher).setChannel(NioDatagramChannel.class).setScheduler(tcpEndpoint.getScheduler()).addHandler(lh)
                .addHandler(new AbstractHandler() {
                    @Override
                    public void onMessageReceived(HandlerContext context, Object message) {
                        context.fireMessageSent(message);
                    }
                }).bind(3001);

        new EndpointBootstrap().setDispatcher(dispatcher).setChannel(NioDatagramChannel.class).setScheduler(tcpEndpoint.getScheduler()).addHandler(lh)
                .addHandler(new AbstractHandler() {
                    @Override
                    public void onOpen(HandlerContext context) {
                        context.fireMessageSent(new Datagram(context.channel().getRemoteAddress(), ByteBuffer.wrap("datagram".getBytes())));
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
        public void onMessageSent(HandlerContext context, String message, ChannelFuture channelFuture) {
            ByteBuffer buffer = charset.encode(message);
            byte[] messageBytes = new byte[buffer.limit()];
            buffer.get(messageBytes);
            buffer.clear();
            context.fireMessageSent(messageBytes, channelFuture);
        }
    }

    private static class MainHandler extends AbstractHandler<String, Object> {

        private static final Logger logger = LoggerFactory.getLogger(MainHandler.class);

        @Override
        public void onMessageReceived(HandlerContext context, String message) {
            context.fireMessageSent("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
            context.fireMessageSent(new Date().toString() + "\r\n");
            if (context.channel() instanceof NioSocketChannel) {
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
            logger.info("Exception [{}] caught on channel [{}]", e, context.channel());
            logger.error(e.getMessage(), e);
            context.fireExceptionCaught(e);
        }

        @Override
        public void onClose(HandlerContext context) {
            logger.info("Channel [{}] closed", context.channel());
            context.fireClose();
        }

        @Override
        public void onClosing(HandlerContext context, final ChannelFuture channelFuture) {
            logger.info("Closing channel [{}]", context.channel());
            channelFuture.addListener(new ChannelListener<Channel>() {
                @Override
                public void onComplete(Channel channel) {
                    logger.info("ChannelFuture [{}] notified: channel [{}] closed", channelFuture, channel);
                }

                @Override
                public void onError(Channel channel, Throwable e) {
                }
            });
            context.fireClosing(channelFuture);
        }

        @Override
        public void onMessageSent(final HandlerContext context, final Object message, final ChannelFuture channelFuture) {
            logger.info("Sending message [{}] to channel [{}]", message, context.channel());
            channelFuture.addListener(new ChannelListener<Channel>() {
                @Override
                public void onComplete(Channel channel) {
                    logger.info("ChannelFuture [{}] notified: message [{}] sent to channel [{}]", channelFuture, message, context.channel());
                }

                @Override
                public void onError(Channel channel, Throwable e) {

                }
            });
            context.fireMessageSent(message, channelFuture);
        }

        @Override
        public void onMessageReceived(HandlerContext context, Object message) {
            logger.info("Received message [{}] from channel [{}]", message, context.channel());
            context.fireMessageReceived(message);
        }

        @Override
        public void onOpen(HandlerContext context) {
            logger.info("Channel [{}] opened", context.channel());
            context.fireOpen();
        }

        @Override
        public void onUnregistered(HandlerContext context) {
            logger.info("Channel [{}] unregistered", context.channel());
            context.fireUnregistered();
        }

        @Override
        public void onRegistered(HandlerContext context) {
            logger.info("Channel [{}] registered", context.channel());
            context.fireRegistered();
        }
    }
}
