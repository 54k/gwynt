package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.IOReactor;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.nio.NioServerSocketChannel;
import io.gwynt.core.pipeline.HandlerContext;

import java.util.Date;

public class GwyntSimpleServer implements Runnable {

    @Override
    public void run() {
        EventLoopGroup eventLoop = new NioEventLoopGroup(2);
        Class<? extends Channel> clazz = NioServerSocketChannel.class;
        final IOReactor reactor = new IOReactor().channelClass(clazz).group(eventLoop)/*.addServerHandler(new LoggingHandler()).addChildHandler(new LoggingHandler())*/
                .addChildHandler(new UtfStringConverter()).addChildHandler(new AbstractHandler<String, Object>() {
                    @Override
                    public void onMessageReceived(HandlerContext context, String message) {
                        context.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
                        context.write(new Date().toString() + "\r\n").addListener(new ChannelFutureListener() {
                            @Override
                            public void onComplete(ChannelFuture future) {
                                future.channel().close();
                            }
                        });
                    }

                    @Override
                    public void onExceptionCaught(HandlerContext context, Throwable e) {
                        e.printStackTrace();
                    }
                });

        try {
            reactor.bind(3001).sync();
        } catch (InterruptedException ignore) {
        }

        //        GlobalEventExecutor.INSTANCE.schedule(new Runnable() {
        //            @Override
        //            public void run() {
        //                reactor.shutdownGracefully();
        //            }
        //        }, 15, TimeUnit.SECONDS);
    }
}
