package io.gwynt.core;

import io.gwynt.core.pipeline.Pipeline;

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

    ChannelFuture disconnect();

    ChannelFuture read();

    ChannelFuture write(Object message, ChannelPromise channelPromise);

    ChannelFuture write(Object message);

    ChannelFuture close();

    ChannelFuture closeFuture();

    ChannelFuture register(EventLoop eventLoop);

    ChannelFuture unregister();

    ChannelPromise voidPromise();

    ByteBufferPool byteBufferPool();

    interface Unsafe<T> {

        T javaChannel();

        void bind(InetSocketAddress address, ChannelPromise channelPromise);

        void connect(InetSocketAddress address, ChannelPromise channelPromise);

        void disconnect(ChannelPromise channelPromise);

        void read(ChannelPromise channelPromise);

        void write(Object message, ChannelPromise channelPromise);

        void close(ChannelPromise channelFuture);

        void register(EventLoop eventLoop);

        void unregister();

        void doRead();

        void doWrite();

        void doConnect();

        ChannelFuture closeFuture();

        SocketAddress getLocalAddress() throws Exception;

        SocketAddress getRemoteAddress() throws Exception;
    }
}
