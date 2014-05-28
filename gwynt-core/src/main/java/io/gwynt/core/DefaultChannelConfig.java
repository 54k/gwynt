package io.gwynt.core;

public class DefaultChannelConfig implements ChannelConfig {

    private Channel channel;
    private boolean autoRead = true;
    private RecvByteBufferAllocator recvByteBufferAllocator = AdaptiveRecvByteBufferAllocator.DEFAULT;
    private ByteBufferPool byteBufferPool = ArrayByteBufferPool.DEFAULT;
    private int writeSpinCount = 8;
    private int readSpinCount = 8;
    private long connectionTimeoutMillis = 0;

    public DefaultChannelConfig(Channel channel) {
        this.channel = channel;
    }

    @Override
    public boolean isAutoRead() {
        return autoRead;
    }

    @Override
    public ChannelConfig setAutoRead(boolean autoRead) {
        this.autoRead = autoRead;
        return this;
    }

    @Override
    public ByteBufferPool getByteBufferPool() {
        return byteBufferPool;
    }

    @Override
    public ChannelConfig setByteBufferPool(ByteBufferPool byteBufferPool) {
        this.byteBufferPool = byteBufferPool;
        return this;
    }

    @Override
    public int getWriteSpinCount() {
        return writeSpinCount;
    }

    @Override
    public ChannelConfig setWriteSpinCount(int writeSpinCount) {
        if (writeSpinCount < 1 || writeSpinCount > 16) {
            throw new IllegalArgumentException("Must be in range [1...16]");
        }
        this.writeSpinCount = writeSpinCount;
        return this;
    }

    @Override
    public int getReadSpinCount() {
        return readSpinCount;
    }

    @Override
    public ChannelConfig setReadSpinCount(int readSpinCount) {
        if (readSpinCount < 1 || readSpinCount > 16) {
            throw new IllegalArgumentException("Must be in range [1...16]");
        }
        this.readSpinCount = readSpinCount;
        return this;
    }

    @Override
    public RecvByteBufferAllocator getRecvByteBufferAllocator() {
        return recvByteBufferAllocator;
    }

    @Override
    public ChannelConfig setRecvByteBufferAllocator(RecvByteBufferAllocator recvByteBufferAllocator) {
        this.recvByteBufferAllocator = recvByteBufferAllocator;
        return this;
    }

    @Override
    public long getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    @Override
    public void setConnectionTimeoutMillis(long connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }
}
