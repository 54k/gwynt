package io.gwynt.core;

import io.gwynt.core.buffer.ByteBufferPool;
import io.gwynt.core.buffer.RecvByteBufferAllocator;

public interface ChannelConfig {

    boolean isAutoRead();

    ChannelConfig setAutoRead(boolean value);

    ByteBufferPool getByteBufferPool();

    ChannelConfig setByteBufferPool(ByteBufferPool byteBufferPool);

    int getWriteSpinCount();

    ChannelConfig setWriteSpinCount(int writeSpinCount);

    int getReadSpinCount();

    ChannelConfig setReadSpinCount(int readSpinCount);

    RecvByteBufferAllocator getRecvByteBufferAllocator();

    ChannelConfig setRecvByteBufferAllocator(RecvByteBufferAllocator byteBufferAllocator);

    int getConnectTimeoutMillis();

    void setConnectTimeoutMillis(int connectTimeoutMillis);
}
