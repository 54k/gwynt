package io.gwynt.http;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelInitializer;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.IOReactor;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.nio.NioServerSocketChannel;
import org.junit.Test;

public class HttpRequestDecoderTest {

    @Test
    public void testHttpRequestDecoder() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup(1);
        IOReactor reactor = new IOReactor().channelClass(NioServerSocketChannel.class).group(group).addHandler(new ChannelInitializer() {
            @Override
            protected void initialize(Channel channel) {
                channel.pipeline().addLast(new HttpRequestDecoder());
            }
        });

        reactor.bind(3000).sync().channel().closeFuture().sync();
    }
}
