package io.gwynt.example;

import io.gwynt.core.ByteHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.Datagram;
import io.gwynt.core.DatagramHandler;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.IOReactor;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.pipeline.HandlerContext;
import io.gwynt.core.rudp.NioRudpChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup(1);
        IOReactor prototype = new IOReactor().channelClass(NioRudpChannel.class).group(group);

        IOReactor server = prototype.clone();
        server.addChildHandler(new DatagramHandler() {
            @Override
            public void onMessageReceived(HandlerContext context, Datagram message) {
                context.write(message);
            }
        });
        server.bind(3001).sync();

        IOReactor client = prototype.clone();
        client.addChildHandler(new ByteHandler() {
            @Override
            public void onMessageReceived(HandlerContext context, byte[] message) {
                System.out.println(new String(message));
            }
        });
        Channel channel = client.connect("localhost", 3001).sync().channel();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.isEmpty()) {
                    channel.write(line.getBytes());
                }
            }
        }
    }
}
