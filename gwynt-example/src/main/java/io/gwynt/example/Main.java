package io.gwynt.example;

import io.gwynt.core.ByteHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.IOReactor;
import io.gwynt.core.buffer.Buffers;
import io.gwynt.core.nio.NioDatagramChannel;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.pipeline.HandlerContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) throws Exception {

        new GwyntSimpleChatServer().run();

        EventLoopGroup group = new NioEventLoopGroup(1);

        //                IOReactor server = new IOReactor().channelClass(NioRudpServerChannel.class).group(group);
        //                server.addChildHandler(new LoggingHandler()).addChildHandler(new ByteHandler() {
        //                    @Override
        //                    public void onMessageReceived(HandlerContext context, byte[] message) {
        //                        context.write(message);
        //                    }
        //
        //                    @Override
        //                    public void onExceptionCaught(HandlerContext context, Throwable e) {
        //                        e.printStackTrace();
        //                    }
        //                });
        //                server.bind(3001).sync();

        final AtomicInteger localSeq = new AtomicInteger();
        final AtomicInteger remoteSeq = new AtomicInteger();

        IOReactor client = new IOReactor().channelClass(NioDatagramChannel.class).group(group);
        client.addChildHandler(new ByteHandler() {
            @Override
            public void onMessageReceived(HandlerContext context, byte[] message) {
                //                ByteBuffer buf = ByteBuffer.wrap(message);
                //                remoteSeq.set(buf.getInt());
                //                int ack = buf.getInt();
                //                byte[] m = new byte[buf.remaining()];
                //                buf.get(m);
                //                System.out.println("=== RCV PACKET ===");
                //                System.out.println("SEQ: " + remoteSeq.get());
                //                System.out.println("ACK: " + ack);
                //                System.out.println("MSG: " + new String(m));
                //                System.out.println("=== END PACKET ===");
                System.out.println(new String(message));
            }
        });
        Channel channel = client.connect("localhost", 3008).sync().channel();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.isEmpty()) {
                    final int lseq = localSeq.getAndIncrement();
                    final byte[] mes = line.getBytes();
                    ByteBuffer buf = ByteBuffer.allocate(/*8 + */mes.length + 2);
                    //                    buf.putInt(lseq);
                    //                    buf.putInt(remoteSeq.get());
                    buf.put(mes);
                    buf.put(new byte[]{'\r', '\n'});
                    buf.flip();

                    channel.write(Buffers.getBytes(buf)).addListener(new ChannelFutureListener() {
                        @Override
                        public void onComplete(ChannelFuture future) {
                            if (future.isSuccess()) {
                                //                                System.out.println("=== SND PACKET ===");
                                //                                System.out.println("SEQ: " + lseq);
                                //                                System.out.println("ACK: " + remoteSeq.get());
                                //                                System.out.println("MSG: " + new String(mes));
                                //                                System.out.println("=== END PACKET ===");
                            }
                        }
                    });
                }
            }
        }
    }
}
