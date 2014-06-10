package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelInitializer;
import io.gwynt.core.Endpoint;
import io.gwynt.core.EndpointBootstrap;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.group.ChannelGroup;
import io.gwynt.core.group.DefaultChannelGroup;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.nio.NioServerSocketChannel;
import io.gwynt.core.nio.NioSocketChannel;
import io.gwynt.core.pipeline.HandlerContext;

import java.net.InetSocketAddress;

public class SimpleGwyntCluster implements Runnable {

    @Override
    public void run() {
        final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        final InetSocketAddress[] addresses = new InetSocketAddress[5];

        for (int i = 0; i < addresses.length; i++) {
            final int port = 9000 + i;
            Endpoint endpoint = new EndpointBootstrap();
            endpoint.group(eventLoopGroup);
            endpoint.channelClass(NioServerSocketChannel.class);
            endpoint.addHandler(new ChannelInitializer() {
                @Override
                protected void initialize(Channel channel) {
                    Endpoint client = new EndpointBootstrap();
                    client.group(eventLoopGroup);
                    client.channelClass(NioSocketChannel.class);
                    channel.pipeline().addLast(new ServerHandler());
                }
            });

            try {
                endpoint.bind(port).sync().channel().getRemoteAddress();
                addresses[i] = new InetSocketAddress(port);
            } catch (InterruptedException ignore) {
            }
        }
    }

    private static final class ServerHandler extends AbstractHandler<byte[], byte[]> {

        private final ChannelGroup channels = new DefaultChannelGroup("clusters");

        @Override
        public void onOpen(HandlerContext context) {
            channels.add(context.channel());
        }

        @Override
        public void onClose(HandlerContext context) {
            channels.remove(context.channel());
        }

        @Override
        public void onMessageReceived(HandlerContext context, byte[] message) {
            channels.remove(context.channel());
        }
    }
}
