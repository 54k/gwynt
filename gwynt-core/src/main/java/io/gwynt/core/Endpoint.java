package io.gwynt.core;

import java.net.InetSocketAddress;

public interface Endpoint {

    Endpoint addHandler(Handler handler);

    Endpoint removeHandler(Handler handler);

    Iterable<Handler> handlers();

    ChannelFactory channelFactory();

    Endpoint channelFactory(ChannelFactory channelFactory);

    EventLoopGroup primaryGroup();

    EventLoopGroup secondaryGroup();

    Endpoint group(EventLoopGroup group);

    Endpoint group(EventLoopGroup primaryGroup, EventLoopGroup secondaryGroup);

    Class<? extends Channel> channelClass();

    Endpoint channelClass(Class<? extends Channel> channel);

    Channel newChannel();

    ChannelFuture bind(int port);

    ChannelFuture connect(String host, int port);

    ChannelFuture connect(InetSocketAddress address);

    Endpoint shutdown();
}
