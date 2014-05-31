package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Endpoint;
import io.gwynt.core.EndpointBootstrap;
import io.gwynt.core.EventLoop;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.nio.NioServerSocketChannel;
import io.gwynt.core.pipeline.HandlerContext;

import java.util.Date;

public class GwyntSimpleServer implements Runnable {

    @Override
    public void run() {
        EventLoop eventLoop = new NioEventLoopGroup();
        Endpoint endpoint = new EndpointBootstrap().setChannelClass(NioServerSocketChannel.class).setEventLoop(eventLoop).addHandler(new UtfStringConverter())
                .addHandler(new AbstractHandler() {
                    @Override
                    public void onMessageReceived(HandlerContext context, Object message) {
                        context.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
                        context.write(new Date().toString() + "\r\n");
                        context.close();
                    }
                });

        try {
            endpoint.bind(3001).sync();
        } catch (InterruptedException ignore) {
        }
    }
}
