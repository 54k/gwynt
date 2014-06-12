package io.gwynt.core.codec;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.ByteBufferPool;
import io.gwynt.core.exception.DecoderException;
import io.gwynt.core.pipeline.HandlerContext;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class ByteToMessageDecoder extends AbstractHandler<byte[], Object> {

    private final List<Object> out = new ArrayList<>();
    private ByteBuffer messageBuffer;

    @Override
    public void onMessageReceived(HandlerContext context, byte[] message) {
        boolean first = messageBuffer == null;

        if (first) {
            initMessageBuffer(context, message);
        } else {
            if (messageBuffer.remaining() < message.length) {
                expandMessageBuffer(context, message.length);
            }
            messageBuffer.mark();
            messageBuffer.put(message);
            messageBuffer.reset();
        }

        callDecode(context);
    }

    private void initMessageBuffer(HandlerContext context, byte[] message) {
        ByteBufferPool allocator = context.channel().config().getByteBufferPool();
        ByteBuffer alloc = allocator.acquire(message.length, false);
        alloc.put(message);
        alloc.flip();
        messageBuffer = alloc;
    }

    private void expandMessageBuffer(HandlerContext context, int readable) {
        ByteBufferPool allocator = context.channel().config().getByteBufferPool();
        ByteBuffer alloc = allocator.acquire(messageBuffer.remaining() + readable, messageBuffer.isDirect());
        alloc.put(messageBuffer);
        allocator.release(messageBuffer);
        messageBuffer = alloc;
    }

    @Override
    public void onHandlerRemoved(HandlerContext context) {
        cleanup(context);
    }

    @Override
    public void onClose(HandlerContext context) {
        cleanup(context);
        context.fireClose();
    }

    private void cleanup(HandlerContext context) {
        if (messageBuffer.hasRemaining()) {
            callDecode(context);
        }
        ByteBufferPool allocator = context.channel().config().getByteBufferPool();
        allocator.release(messageBuffer);
        if (!out.isEmpty()) {
            out.clear();
        }
        messageBuffer = null;
    }

    private void callDecode(HandlerContext context) {
        try {
            decode(context, messageBuffer, out);
        } catch (DecoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new DecoderException(e);
        } finally {
            for (Object m : out) {
                context.fireMessageReceived(m);
            }
            out.clear();
        }
    }

    protected abstract void decode(HandlerContext context, ByteBuffer message, List<Object> out);
}
