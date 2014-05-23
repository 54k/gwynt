package io.gwynt.core;

import io.gwynt.core.pipeline.Pipeline;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public interface Channel {

    Channel parent();

    boolean isRegistered();

    ChannelConfig config();

    Object attach(Object attachment);

    Object attachment();

    Pipeline pipeline();

    EventLoop eventLoop();

    SocketAddress getLocalAddress();

    SocketAddress getRemoteAddress();

    Unsafe unsafe();

    ChannelPromise newChannelPromise();

    ChannelFuture bind(InetSocketAddress address);

    ChannelFuture connect(InetSocketAddress address);

    ChannelFuture read();

    ChannelFuture write(Object message);

    ChannelFuture close();

    ChannelFuture closeFuture();

    ChannelFuture register(EventLoop eventLoop);

    ChannelFuture unregister();

    Object javaChannel();

    interface Unsafe<T> {

        T javaChannel();

        void bind(InetSocketAddress address, ChannelPromise channelPromise);

        void connect(InetSocketAddress address, ChannelPromise channelPromise);

        void read(ChannelPromise channelPromise);

        void write(Object message, ChannelPromise channelPromise);

        void close(ChannelPromise channelFuture);

        void register(EventLoop eventLoop);

        void unregister();

        void doRead() throws IOException;

        void doWrite() throws IOException;

        void doConnect() throws IOException;

        void exceptionCaught(Throwable e);

        ChannelFuture closeFuture();

        SocketAddress getLocalAddress() throws Exception;

        SocketAddress getRemoteAddress() throws Exception;
    }
}
