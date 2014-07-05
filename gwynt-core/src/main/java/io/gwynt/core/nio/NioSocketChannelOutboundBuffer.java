package io.gwynt.core.nio;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelOutboundBuffer;

import java.nio.ByteBuffer;

final class NioSocketChannelOutboundBuffer extends ChannelOutboundBuffer {

    public NioSocketChannelOutboundBuffer(Channel channel) {
        super(channel);
    }

    public ByteBuffer[] byteBuffers() {
        ByteBuffer[] byteBuffers = new ByteBuffer[entries().size()];
        int i = 0;
        for (Entry entry : entries()) {
            byteBuffers[i++] = (ByteBuffer) entry.getMessage();
        }
        return byteBuffers;
    }

    public long remaining() {
        long bytes = 0;
        for (Entry entry : entries()) {
            bytes += ((ByteBuffer) entry.getMessage()).remaining();
        }
        return bytes;
    }

    @Override
    protected Object prepareMessage(Object message) {
        ByteBuffer buffer;
        if (message instanceof byte[]) {
            byte[] bytes = (byte[]) message;
            buffer = channel().config().getByteBufferPool().acquire(bytes.length, false);
            buffer.put(bytes);
            buffer.flip();
        } else if (message instanceof ByteBuffer) {
            ByteBuffer byteBuffer = (ByteBuffer) message;
            byteBuffer.flip();
            buffer = channel().config().getByteBufferPool().acquire(byteBuffer.limit(), false);
            buffer.put(byteBuffer);
            buffer.flip();
        } else {
            throw new IllegalArgumentException("Wrong message type");
        }
        return buffer;
    }

    @Override
    protected void clearEntry(Entry entry) {
        channel().config().getByteBufferPool().release((ByteBuffer) entry.getMessage());
    }
}
