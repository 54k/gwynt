package io.gwynt.core;

import io.gwynt.core.pipeline.Pipeline;
import io.gwynt.core.scheduler.EventScheduler;
import io.gwynt.core.transport.Dispatcher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;

public interface Channel {

    Channel parent();

    boolean isRegistered();

    Object attach(Object attachment);

    Object attachment();

    Pipeline pipeline();

    EventScheduler scheduler();

    SocketAddress getLocalAddress();

    SocketAddress getRemoteAddress();

    Unsafe unsafe();

    Dispatcher dispatcher();

    Endpoint endpoint();

    ChannelPromise newChannelPromise();

    ChannelFuture bind(InetSocketAddress address);

    ChannelFuture connect(InetSocketAddress address);

    ChannelFuture read();

    ChannelFuture write(Object message);

    ChannelFuture close();

    ChannelFuture closeFuture();

    ChannelFuture register(Dispatcher dispatcher);

    ChannelFuture unregister();

    interface Unsafe<T extends SelectableChannel> {

        T javaChannel();

        ChannelFuture bind(InetSocketAddress address, ChannelPromise channelPromise);

        ChannelFuture connect(InetSocketAddress address, ChannelPromise channelPromise);

        void read(ChannelPromise channelPromise);

        ChannelFuture write(Object message, ChannelPromise channelPromise);

        ChannelFuture close(ChannelPromise channelFuture);

        void doRegister(Dispatcher dispatcher);

        void doUnregister(Dispatcher dispatcher);

        void doAccept() throws IOException;

        void doRead() throws IOException;

        void doWrite() throws IOException;

        void doConnect() throws IOException;

        void exceptionCaught(Throwable e);

        ChannelFuture closeFuture();
    }
}
