package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Datagram;
import io.gwynt.core.IOReactor;
import io.gwynt.core.nio.NioDatagramChannel;
import io.gwynt.core.nio.NioEventLoop;
import io.gwynt.core.pipeline.HandlerContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

//import io.netty.channel.nio.NioEventLoopGroup;

//import io.gwynt.core.Channel;
//import io.gwynt.core.Datagram;
//import java.io.BufferedReader;
//import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) throws Exception {
        new NettySimpleServer().run();
        new GwyntSimpleServer().run();
        //                new MinaSimpleServer().run();

        new GwyntSimpleChatServer().run();

        //        new NioEventLoopGroup(1).scheduleAtFixedRate(new Runnable() {
        //            @Override
        //            public void run() {
        //                System.out.println("tick");
        //            }
        //        }, 0, 2000, TimeUnit.MILLISECONDS);
        final StringConverter sc = new StringConverter();
        final LoggingHandler lh = new LoggingHandler();
        //                        final EchoHandler eh = new EchoHandler();

        NioEventLoop dispatcher = new NioEventLoop();
        //
        //                        Endpoint tcpEndpoint = new EndpointBootstrap().group(dispatcher).channelClass(NioServerSocketChannel.class).addHandler(sc).addHandler(lh).addHandler(eh);
        //                        tcpEndpoint.bind(3002).await();
        //
        //                        NioEventLoop dispatcher2 = new NioEventLoop();
        //
        //                        final Endpoint tcpClient = new EndpointBootstrap();
        //                        tcpClient.group(dispatcher2).channelClass(NioSocketChannel.class).addHandler(new ChannelInitializer() {
        //
        //                            @Override
        //                            protected void initialize(Channel session) {
        //                                session.pipeline().addLast(new ConnectionWatchDog(tcpClient));
        //                                session.pipeline().addLast(sc);
        //                                session.pipeline().addLast(new AbstractHandler<String, String>() {
        //                                    private Logger logger = LoggerFactory.getLogger(getClass());
        //
        //                                    @Override
        //                                    public void onOpen(HandlerContext context) {
        //                                        context.write("@echo");
        //                                    }
        //
        //                                    @Override
        //                                    public void onMessageReceived(HandlerContext context, String message) {
        //                                                /*context.write(message);*/
        //                                    }
        //
        //                                    @Override
        //                                    public void onExceptionCaught(HandlerContext context, Throwable e) {
        //                                        logger.error(e.getMessage(), e);
        //                                    }
        //                                });
        //                            }
        //                        }).connect("localhost", 3002).await();
        //
        //                        for (int i = 0; i < 5; i++) {
        //                            Thread.sleep(10);
        //                            tcpEndpoint.shutdown();
        //                            Thread.sleep(1);
        //                            tcpEndpoint.bind(3002).await(10, TimeUnit.MILLISECONDS);
        //                        }

        new IOReactor().channelClass(NioDatagramChannel.class).group(dispatcher).addHandler(lh).addHandler(new AbstractHandler() {
            @Override
            public void onMessageReceived(HandlerContext context, Object message) {
                context.write(message);
            }
        }).bind(3002).await();

        new IOReactor().channelClass(NioDatagramChannel.class).group(dispatcher).addHandler(lh).addHandler(new AbstractHandler() {
            @Override
            public void onOpen(HandlerContext context) {
                context.write(new Datagram(ByteBuffer.wrap("datagram".getBytes()), context.channel().getRemoteAddress()));
            }

            @Override
            public void onMessageReceived(HandlerContext context, Object message) {
                context.write(message);
                context.close();
            }
        }).connect("localhost", 3002).await();

        Channel channel = new IOReactor().channelClass(NioDatagramChannel.class).group(dispatcher).addHandler(sc).addHandler(new AbstractHandler() {
            @Override
            public void onMessageReceived(HandlerContext context, Object message) {
                System.out.println(message);
            }
        }).connect("localhost", 3002).await().channel();

        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void onComplete(ChannelFuture channelFuture) {
                System.exit(0);
            }
        });
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = br.readLine()) != null) {
                channel.write(line + "\r\n");
            }
        }
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

}
