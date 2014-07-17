package io.gwynt.core;

import io.gwynt.core.buffer.ByteBufferPool;
import io.gwynt.core.pipeline.Pipeline;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public interface Channel {

    Channel parent();

    boolean isRegistered();

    boolean isActive();

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

    Object javaChannel();

    interface Unsafe<T> {

        T javaChannel();

        boolean isOpen();

        boolean isActive();

        void bind(InetSocketAddress address, ChannelPromise channelPromise);

        void connect(InetSocketAddress address, ChannelPromise channelPromise);

        void disconnect(ChannelPromise channelPromise);

        void read(ChannelPromise channelPromise);

        void write(Object message, ChannelPromise channelPromise);

        void flush();

        void close(ChannelPromise channelFuture);

        void closeForcibly();

        void register(EventLoop eventLoop);

        void unregister();

        ChannelFuture closeFuture();

        SocketAddress getLocalAddress() throws Exception;

        SocketAddress getRemoteAddress() throws Exception;
    }
}
