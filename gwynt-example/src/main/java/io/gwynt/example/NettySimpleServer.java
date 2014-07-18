package io.gwynt.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Date;

public class NettySimpleServer implements Runnable {

    @Override
    public void run() {
        final StringDecoder decoder = new StringDecoder();
        final StringEncoder encoder = new StringEncoder();

        EventLoopGroup eventLoop = new OioEventLoopGroup();
        ServerBootstrap serverBootstrap =
                new ServerBootstrap().channel(io.netty.channel.socket.oio.OioServerSocketChannel.class).group(eventLoop).childHandler(new ChannelInitializer<OioSocketChannel>() {
            @Override
            protected void initChannel(final OioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(decoder);
                ch.pipeline().addLast(encoder);
                ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                    @Override
                    protected void channelRead0(final ChannelHandlerContext ctx, String msg) throws Exception {
                        ctx.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
                        ctx.writeAndFlush(new Date().toString() + "\r\n").addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                if (future.isSuccess()) {
                                    ctx.close();
                                }
                            }
                        });
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        //                        System.out.println("Netty: " + ctx.channel() + " closed");
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
