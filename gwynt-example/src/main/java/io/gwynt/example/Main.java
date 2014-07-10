package io.gwynt.example;

//import io.gwynt.core.Datagram;
//import io.gwynt.core.DatagramHandler;
//import io.gwynt.core.EventLoopGroup;
//import io.gwynt.core.IOReactor;
//import io.gwynt.core.nio.NioDatagramChannel;
//import io.gwynt.core.nio.NioEventLoopGroup;
//import io.gwynt.core.pipeline.HandlerContext;

public class Main {

    public static void main(String[] args) throws Exception {
        new GwyntSimpleChatServer().run();
        //        EventLoopGroup group = new NioEventLoopGroup(1);
        //        IOReactor prototype = new IOReactor().channelClass(NioDatagramChannel.class).group(group);
        //
        //        IOReactor server = prototype.clone();
        //        server.addChildHandler(new DatagramHandler() {
        //            @Override
        //            public void onMessageReceived(HandlerContext context, Datagram message) {
        //
        //            }
        //        });
        //        server.bind(3001).sync();
        //
        //        IOReactor client = prototype.clone();
        //        client.connect("localhost", 3001).sync();
    }
}
