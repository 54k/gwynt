package io.gwynt.core;

public interface ChannelConfig {

    boolean isAutoRead();

    ChannelConfig setAutoRead(boolean value);

    ByteBufferPool getByteBufferPool();

    ChannelConfig setByteBufferPool(ByteBufferPool byteBufferPool);
}
