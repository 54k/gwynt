package io.gwynt.http;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelInitializer;
import io.gwynt.core.Endpoint;
import io.gwynt.core.EndpointBootstrap;
import io.gwynt.core.EventLoopGroup;
import io.gwynt.core.nio.NioEventLoopGroup;
import io.gwynt.core.nio.NioServerSocketChannel;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class HttpRequestDecoderTest {

    @Test
    public void testHttpRequestDecoder() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup(1);
        Endpoint endpoint = new EndpointBootstrap().channelClass(NioServerSocketChannel.class).group(group).addHandler(new ChannelInitializer() {
            @Override
            protected void initialize(Channel channel) {
                channel.pipeline().addLast(new HttpRequestDecoder());
            }
        });

        endpoint.bind(3000).sync();

        new CountDownLatch(1).await();
    }
}
