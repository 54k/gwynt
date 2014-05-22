package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.pipeline.HandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class Main {

    public static void main(String[] args) throws Exception {
        new NettySimpleServer().run();
        new GwyntSimpleServer().run();
//
//        StringConverter sc = new StringConverter();
//        LoggingHandler lh = new LoggingHandler();
//        EchoHandler eh = new EchoHandler();
//
//        NioEventLoopGroup dispatcher = new NioEventLoopGroup();
//        dispatcher.runThread();
//
//        Endpoint tcpEndpoint = new EndpointBootstrap().setScheduler(dispatcher).setChannelClass(NioServerSocketChannel.class).addHandler(sc).addHandler(lh).addHandler(eh);
//        tcpEndpoint.bind(3002).await();
//
//        new EndpointBootstrap().setScheduler(tcpEndpoint.getScheduler()).setChannelClass(NioSocketChannel.class).addHandler(sc).addHandler(lh)
//                .addHandler(new AbstractHandler<String, String>() {
//                    private Logger logger = LoggerFactory.getLogger(getClass());
//
//                    @Override
//                    public void onOpen(HandlerContext context) {
//                        context.write("echo echo echo");
//                    }
//
//                    @Override
//                    public void onMessageReceived(HandlerContext context, String message) {
//                        context.write(message);
//                        context.close();
//                    }
//
//                    @Override
//                    public void onExceptionCaught(HandlerContext context, Throwable e) {
//                        logger.error(e.getMessage(), e);
//                    }
//                }).connect("localhost", 3002).await();
//
//        new EndpointBootstrap().setChannelClass(NioDatagramChannel.class).setScheduler(tcpEndpoint.getScheduler()).addHandler(lh).addHandler(new AbstractHandler() {
//            @Override
//            public void onMessageReceived(HandlerContext context, Object message) {
//                context.write(message);
//            }
//        }).bind(3002).await();
//
//        new EndpointBootstrap().setChannelClass(NioDatagramChannel.class).setScheduler(tcpEndpoint.getScheduler()).addHandler(lh).addHandler(new AbstractHandler() {
//            @Override
//            public void onOpen(HandlerContext context) {
//                context.write(new Datagram(context.channel().getRemoteAddress(), ByteBuffer.wrap("datagram".getBytes())));
//            }
//
//            @Override
//            public void onMessageReceived(HandlerContext context, Object message) {
//                context.write(message);
//                context.close();
//            }
//        }).connect("localhost", 3002).await();
//
//        Channel channel = new EndpointBootstrap().setChannelClass(NioSocketChannel.class).addHandler(sc).addHandler(new AbstractHandler() {
//            @Override
//            public void onMessageReceived(HandlerContext context, Object message) {
//                System.out.println(message);
//            }
//        }).connect("localhost", 3002).await().channel();
//
//        channel.closeFuture().addListener(new ChannelFutureListener() {
//            @Override
//            public void onComplete(ChannelFuture channelFuture) {
//                System.exit(0);
//            }
//
//            @Override
//            public void onError(ChannelFuture channelFuture, Throwable e) {
//            }
//        });
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                channel.write(line + "\r\n");
//            }
//        }
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
        public void onMessageSent(HandlerContext context, String message, ChannelPromise channelPromise) {
            ByteBuffer buffer = charset.encode(message);
            byte[] messageBytes = new byte[buffer.limit()];
            buffer.get(messageBytes);
            buffer.clear();
            context.write(messageBytes, channelPromise);
        }
    }

    private static class EchoHandler extends AbstractHandler<String, Object> {

        @Override
        public void onMessageReceived(HandlerContext context, String message) {
            if ("exit\r\n".equals(message)) {
                context.close();
                return;
            }
            context.write(message);
        }
    }

    private static class LoggingHandler extends AbstractHandler {

        private static final Logger logger = LoggerFactory.getLogger(LoggingHandler.class);

        @Override
        public void onExceptionCaught(HandlerContext context, Throwable e) {
            logger.info("Caught exception [{}] on channel [{}]", e, context.channel());
            logger.error(e.getMessage(), e);
            context.fireExceptionCaught(e);
        }

        @Override
        public void onClose(HandlerContext context) {
            logger.info("Closed channel [{}]", context.channel());
            context.fireClose();
        }

        @Override
        public void onClosing(HandlerContext context, final ChannelPromise channelPromise) {
            logger.info("Closing channel [{}]", context.channel());
            channelPromise.addListener(new ChannelFutureListener() {
                @Override
                public void onComplete(ChannelFuture channelFuture) {
                    logger.info("Closed channel [{}], channelPromise [{}]", channelFuture.channel(), channelFuture);
                }
            });
            context.close(channelPromise);
        }

        @Override
        public void onMessageSent(final HandlerContext context, final Object message, final ChannelPromise channelPromise) {
            logger.info("Sending message [{}] to channel [{}]", message, context.channel());
            channelPromise.addListener(new ChannelFutureListener() {
                @Override
                public void onComplete(ChannelFuture channelFuture) {
                    logger.info("Sent to channel [{}], channelPromise [{}], message [{}]", channelFuture.channel(), channelPromise, message);
                }
            });
            context.write(message, channelPromise);
        }

        @Override
        public void onMessageReceived(HandlerContext context, Object message) {
            logger.info("Received message [{}] from channel [{}]", message, context.channel());
            context.fireMessageReceived(message);
        }

        @Override
        public void onOpen(HandlerContext context) {
            logger.info("Opened channel [{}]", context.channel());
            context.fireOpen();
        }

        @Override
        public void onUnregistered(HandlerContext context) {
            logger.info("Unregistered channel [{}]", context.channel());
            context.fireUnregistered();
        }

        @Override
        public void onRegistered(HandlerContext context) {
            logger.info("Registered Channel [{}]", context.channel());
            context.fireRegistered();
        }
    }
}
