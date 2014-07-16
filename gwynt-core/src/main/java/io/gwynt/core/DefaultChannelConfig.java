package io.gwynt.core;

import io.gwynt.core.buffer.AdaptiveRecvByteBufferAllocator;
import io.gwynt.core.buffer.ArrayByteBufferPool;
import io.gwynt.core.buffer.ByteBufferPool;
import io.gwynt.core.buffer.FixedRecvByteBufferAllocator;
import io.gwynt.core.buffer.RecvByteBufferAllocator;

public class DefaultChannelConfig implements ChannelConfig {

    private Channel channel;
    private boolean autoRead;

    private int writeSpinCount;
    private int readSpinCount;
    private int connectTimeoutMillis;

    private RecvByteBufferAllocator recvByteBufferAllocator;
    private ByteBufferPool byteBufferPool;

    public DefaultChannelConfig(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel");
        }

        this.channel = channel;
        autoRead = true;
        writeSpinCount = 8;
        readSpinCount = 8;
        connectTimeoutMillis = 0;
        byteBufferPool = ArrayByteBufferPool.DEFAULT;
        recvByteBufferAllocator = defaultRecvByteBufferAllocator(channel);
    }

    private static RecvByteBufferAllocator defaultRecvByteBufferAllocator(Channel channel) {
        if (channel instanceof MulticastChannel) {
            return FixedRecvByteBufferAllocator.DEFAULT;
        } else {
            return AdaptiveRecvByteBufferAllocator.DEFAULT;
        }
    }

    protected Channel channel() {
        return channel;
    }

    protected Object javaChannel() {
        return channel.unsafe().javaChannel();
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
    public <T> boolean setOption(ChannelOption<T> channelOption, T value) {
        try {
            boolean isSet = true;
            if (channelOption == ChannelOption.AUTO_READ) {
                setAutoRead((Boolean) value);
            } else if (channelOption == ChannelOption.BYTE_BUFFER_POOL) {
                setByteBufferPool((ByteBufferPool) value);
            } else if (channelOption == ChannelOption.RECV_BYTE_BUFFER_ALLOCATOR) {
                setRecvByteBufferAllocator((RecvByteBufferAllocator) value);
            } else if (channelOption == ChannelOption.READ_SPIN_COUNT) {
                setReadSpinCount((Integer) value);
            } else if (channelOption == ChannelOption.WRITE_SPIN_COUNT) {
                setWriteSpinCount((Integer) value);
            } else if (channelOption == ChannelOption.CONNECT_TIMEOUT_MILLIS) {
                setConnectTimeoutMillis((Integer) value);
            } else {
                isSet = false;
            }

            return setOption0(channelOption, value) || isSet;
        } catch (UnsupportedOperationException e) {
            return false;
        } catch (Throwable e) {
            throw new ChannelException(e);
        }
    }

    protected <T> boolean setOption0(ChannelOption<T> channelOption, T value) throws Exception {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOption(ChannelOption<T> channelOption) {
        try {

            Object result = null;
            if (channelOption == ChannelOption.AUTO_READ) {
                result = isAutoRead();
            } else if (channelOption == ChannelOption.BYTE_BUFFER_POOL) {
                result = getByteBufferPool();
            } else if (channelOption == ChannelOption.RECV_BYTE_BUFFER_ALLOCATOR) {
                result = getRecvByteBufferAllocator();
            } else if (channelOption == ChannelOption.READ_SPIN_COUNT) {
                result = getReadSpinCount();
            } else if (channelOption == ChannelOption.WRITE_SPIN_COUNT) {
                result = getWriteSpinCount();
            } else if (channelOption == ChannelOption.CONNECT_TIMEOUT_MILLIS) {
                result = getConnectTimeoutMillis();
            }

            if (result == null) {
                return getOption0(channelOption);
            }
            return (T) result;
        } catch (UnsupportedOperationException e) {
            return null;
        } catch (Throwable e) {
            throw new ChannelException(e);
        }
    }

    protected <T> T getOption0(ChannelOption<T> channelOption) throws Exception {
        return null;
    }
}
