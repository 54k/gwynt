package io.gwynt.core;

public class DefaultChannelConfig implements ChannelConfig {

    private Channel channel;
    private boolean autoRead = true;
    private ByteBufferPool byteBufferPool = ByteBufferPool.DEFAULT;

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
}
