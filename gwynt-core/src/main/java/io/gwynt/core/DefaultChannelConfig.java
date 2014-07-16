package io.gwynt.core;

import io.gwynt.core.buffer.AdaptiveRecvByteBufferAllocator;
import io.gwynt.core.buffer.ArrayByteBufferPool;
import io.gwynt.core.buffer.ByteBufferPool;
import io.gwynt.core.buffer.FixedRecvByteBufferAllocator;
import io.gwynt.core.buffer.RecvByteBufferAllocator;

public class DefaultChannelConfig implements ChannelConfig {

    private Channel channel;
    private boolean autoRead = true;
    private RecvByteBufferAllocator recvByteBufferAllocator;
    private ByteBufferPool byteBufferPool = ArrayByteBufferPool.DEFAULT;
    private int writeSpinCount = 8;
    private int readSpinCount = 8;
    private int connectTimeoutMillis = 0;

    public DefaultChannelConfig(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel");
        }

        this.channel = channel;
        recvByteBufferAllocator = defaultRecvByteBufferAllocator(channel);
    }

    private static RecvByteBufferAllocator defaultRecvByteBufferAllocator(Channel channel) {
        if (channel instanceof MulticastChannel) {
            return FixedRecvByteBufferAllocator.DEFAULT;
        } else {
            return AdaptiveRecvByteBufferAllocator.DEFAULT;
        }
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
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    @Override
    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    @Override
    public <T> boolean setChannelOption(ChannelOption<T> channelOption, T value) {
        return true;
    }

    @Override
    public <T> T getChannelOption(ChannelOption<T> channelOption) {
        return null;
    }
}
