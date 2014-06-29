package io.gwynt.core.codec;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.ByteBufferPool;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.pipeline.HandlerContext;

import java.nio.ByteBuffer;

public abstract class MessageToByteEncoder<O> extends AbstractHandler<Object, O> {

    @Override
    public void onMessageSent(HandlerContext context, O message, ChannelPromise channelPromise) {
        ByteBufferPool allocator = context.channel().config().getByteBufferPool();
        ByteBuffer out = allocator.acquire(65536, preferDirectBuffer());

        try {
            encode(context, message, out);
            out.flip();
            byte[] bytes = new byte[out.remaining()];
            out.get(bytes);
            context.write(bytes, channelPromise);
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable e) {

            throw new EncoderException(e);
        } finally {
            allocator.release(out);
        }
    }

    protected boolean preferDirectBuffer() {
        return false;
    }

    protected abstract void encode(HandlerContext context, O message, ByteBuffer out);
}
