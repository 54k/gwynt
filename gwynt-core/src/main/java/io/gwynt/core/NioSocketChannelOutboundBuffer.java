package io.gwynt.core;

import java.nio.ByteBuffer;

public class NioSocketChannelOutboundBuffer extends ChannelOutboundBuffer {

    public NioSocketChannelOutboundBuffer(Channel channel) {
        super(channel);
    }

    @Override
    protected Object prepareMessage(Object message) {
        ByteBuffer buffer;
        if (message instanceof byte[]) {
            byte[] bytes = (byte[]) message;
            buffer = channel().config().getByteBufferPool().acquire(bytes.length, true);
            buffer.put(bytes);
            buffer.flip();
        } else if (message instanceof ByteBuffer) {
            ByteBuffer byteBuffer = (ByteBuffer) message;
            byteBuffer.flip();
            buffer = byteBuffer;
        } else {
            throw new IllegalArgumentException("Wrong message type");
        }
        return buffer;
    }
}
