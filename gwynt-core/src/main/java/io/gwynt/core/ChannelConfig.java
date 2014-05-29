package io.gwynt.core;

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

    long getConnectTimeoutMillis();

    void setConnectTimeoutMillis(long connectionTimeoutMillis);
}
