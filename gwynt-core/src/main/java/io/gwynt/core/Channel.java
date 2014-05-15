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

    ChannelFuture newChannelFuture();

    interface Unsafe<T extends SelectableChannel> {

        T javaChannel();

        ChannelFuture bind(InetSocketAddress address);

        ChannelFuture connect(InetSocketAddress address);

        void read();

        void write(Object message, ChannelFuture channelFuture);

        void close(ChannelFuture channelFuture);

        void doRegister(Dispatcher dispatcher);

        void doUnregister(Dispatcher dispatcher);

        void doAccept() throws IOException;

        void doRead() throws IOException;

        void doWrite() throws IOException;

        void doConnect() throws IOException;

        void exceptionCaught(Throwable e);
    }
}
