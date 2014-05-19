package io.gwynt.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Date;

public class NettySimpleServer implements Runnable {

    @Override
    public void run() {
        final StringDecoder decoder = new StringDecoder();
        final StringEncoder encoder = new StringEncoder();

        NioEventLoopGroup eventLoop = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.group(eventLoop);
        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(final NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(decoder);
                ch.pipeline().addLast(encoder);
                ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                        ctx.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
                        ctx.writeAndFlush(new Date().toString() + "\r\n");
                        ctx.close();
                    }
                });
            }
        });

        try {
            serverBootstrap.bind(3000).await();
        } catch (InterruptedException ignore) {
        }
    }
}
