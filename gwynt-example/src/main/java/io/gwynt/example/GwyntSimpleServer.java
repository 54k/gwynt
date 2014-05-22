package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Endpoint;
import io.gwynt.core.EndpointBootstrap;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.nio.NioServerSocketChannel;
import io.gwynt.core.pipeline.HandlerContext;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Date;

public class GwyntSimpleServer implements Runnable {

    @Override
    public void run() {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        Endpoint endpoint = new EndpointBootstrap().setChannelClass(NioServerSocketChannel.class).setScheduler(eventLoopGroup).addHandler(new AbstractHandler<byte[], String>() {
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
        }).addHandler(new AbstractHandler() {
            @Override
            public void onMessageReceived(HandlerContext context, Object message) {
                context.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
                context.write(new Date().toString() + "\r\n");
                context.close();
            }
        });

        endpoint.bind(3002).await();
    }
}
