package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.IOReactor;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.nio.NioServerSocketChannel;
import io.gwynt.core.pipeline.HandlerContext;

import java.util.Date;

public class GwyntSimpleServer implements Runnable {

    @Override
    public void run() {
        EventLoopGroup eventLoop = new NioEventLoopGroup();
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
    }
}
