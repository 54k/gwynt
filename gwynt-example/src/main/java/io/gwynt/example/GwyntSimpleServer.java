package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.IOReactor;
import io.gwynt.core.concurrent.FutureGroup;
import io.gwynt.core.concurrent.FutureGroupListener;
import io.gwynt.core.concurrent.GlobalEventExecutor;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.nio.NioServerSocketChannel;
import io.gwynt.core.pipeline.HandlerContext;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class GwyntSimpleServer implements Runnable {

    @Override
    public void run() {
        final EventLoopGroup eventLoop = new NioEventLoopGroup();
        IOReactor reactor = new IOReactor().channelClass(NioServerSocketChannel.class).group(eventLoop).addHandler(new UtfStringConverter()).addHandler(new AbstractHandler() {
            @Override
            public void onMessageReceived(HandlerContext context, Object message) {
                context.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
                context.write(new Date().toString() + "\r\n");
                context.close();
            }
        });

        try {
            reactor.bind(3001).sync();
        } catch (InterruptedException ignore) {
        }

        GlobalEventExecutor.INSTANCE.schedule(new Runnable() {
            @Override
            public void run() {
                eventLoop.shutdownGracefully().addListener(new FutureGroupListener<Void>() {
                    @Override
                    public void onComplete(FutureGroup<Void> future) {
                        System.out.println("SHUTDOWN");
                    }
                });
            }
        }, 5, TimeUnit.SECONDS);
    }
}
