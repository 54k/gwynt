package io.gwynt.core.rudp;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelOutboundBuffer;
import io.gwynt.core.buffer.ByteBufferPool;
import io.gwynt.core.buffer.DynamicByteBuffer;

import java.nio.ByteBuffer;

final class RudpVirtualChannelOutboundBuffer extends ChannelOutboundBuffer {

    RudpVirtualChannelOutboundBuffer(Channel channel) {
        super(channel);
    }

    @Override
    protected RudpVirtualChannel channel() {
        return (RudpVirtualChannel) super.channel();
    }

    @Override
    protected Object prepareMessage(Object message) {
        ByteBuffer buffer;
        ByteBufferPool alloc = channel().byteBufferPool();

        if (message instanceof byte[]) {
            byte[] bytes = (byte[]) message;
            buffer = alloc.acquire(bytes.length, false);
            buffer.put(bytes);
            buffer.flip();
        } else if (message instanceof ByteBuffer) {
            ByteBuffer byteBuffer = (ByteBuffer) message;
            byteBuffer.flip();
            buffer = alloc.acquire(byteBuffer.limit(), false);
            buffer.put(byteBuffer);
            buffer.flip();
        } else {
            throw new IllegalArgumentException("Wrong message type");
        }

        DynamicByteBuffer dynamicBuffer = alloc.acquireDynamic(8, false);
        dynamicBuffer.putInt(channel().localSeq.getAndIncrement());
        dynamicBuffer.putInt(channel().remoteSeq.get());
        dynamicBuffer.put(buffer);
        dynamicBuffer.flip();
        alloc.release(buffer);

        return dynamicBuffer.asByteBuffer();
    }
}
