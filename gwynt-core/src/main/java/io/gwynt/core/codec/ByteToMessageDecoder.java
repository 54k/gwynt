package io.gwynt.core.codec;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.buffer.ByteBufferPool;
import io.gwynt.core.pipeline.HandlerContext;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class ByteToMessageDecoder extends AbstractHandler<byte[], Object> {

    private final List<Object> out = new ArrayList<>();
    private ByteBuffer internalBuffer;
    private boolean decodeLast;
    private boolean singleDecode;

    protected boolean isSingleDecode() {
        return singleDecode;
    }

    protected void setSingleDecode(boolean singleDecode) {
        this.singleDecode = singleDecode;
    }

    protected ByteBuffer internalBuffer() {
        return internalBuffer;
    }

    @Override
    public void onMessageReceived(HandlerContext context, byte[] message) {
        boolean first = internalBuffer == null;

        if (first) {
            initInternalBuffer(context, message);
        } else {
            if (internalBuffer.remaining() < message.length) {
                expandInternalBuffer(context, message.length);
            }
            internalBuffer.mark();
            internalBuffer.put(message);
            internalBuffer.reset();
        }

        try {
            while (internalBuffer.hasRemaining()) {
                int oldPosition = internalBuffer.position();
                int oldSize = out.size();
                callDecode(context, internalBuffer, out);

                if (oldSize == out.size() && oldPosition == internalBuffer.position()) {
                    throw new DecoderException(getClass().getSimpleName() + "#decode did not decode anything");
                }

                if (isSingleDecode()) {
                    break;
                }
            }
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

    private void initInternalBuffer(HandlerContext context, byte[] message) {
        ByteBufferPool allocator = context.channel().config().getByteBufferPool();
        ByteBuffer alloc = allocator.acquire(message.length, preferDirectBuffer());
        alloc.put(message);
        alloc.flip();
        internalBuffer = alloc;
    }

    private void expandInternalBuffer(HandlerContext context, int readable) {
        ByteBufferPool allocator = context.channel().config().getByteBufferPool();
        ByteBuffer alloc = allocator.acquire(internalBuffer.remaining() + readable, internalBuffer.isDirect());
        alloc.put(internalBuffer);
        allocator.release(internalBuffer);
        internalBuffer = alloc;
    }

    protected boolean preferDirectBuffer() {
        return false;
    }

    @Override
    public void onHandlerRemoved(HandlerContext context) {
        if (internalBuffer == null) {
            return;
        }

        if (internalBuffer.hasRemaining()) {
            byte[] bytes = new byte[internalBuffer.remaining()];
            internalBuffer.get(bytes);
            ByteBufferPool allocator = context.channel().config().getByteBufferPool();
            allocator.release(internalBuffer);
            internalBuffer = null;
            if (!out.isEmpty()) {
                out.clear();
            }
            context.fireMessageReceived(bytes);
        }
        onHandlerRemoved0(context);
    }

    protected void onHandlerRemoved0(HandlerContext context) {
    }

    @Override
    public void onClose(HandlerContext context) {
        if (internalBuffer == null) {
            context.fireClose();
            return;
        }

        decodeLast = true;
        if (internalBuffer.hasRemaining()) {
            callDecode(context, internalBuffer, out);
        }
        ByteBufferPool allocator = context.channel().config().getByteBufferPool();
        allocator.release(internalBuffer);
        if (!out.isEmpty()) {
            for (Object m : out) {
                context.fireMessageReceived(m);
            }
            out.clear();
        }
        internalBuffer = null;
        context.fireClose();
    }

    protected void callDecode(HandlerContext context, ByteBuffer in, List<Object> out) {
        try {
            if (decodeLast) {
                decodeLast(context, in, out);
            } else {
                decode(context, in, out);
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new DecoderException(e);
        }
    }

    protected abstract void decode(HandlerContext context, ByteBuffer message, List<Object> out);

    protected void decodeLast(HandlerContext context, ByteBuffer message, List<Object> out) {
        decode(context, message, out);
    }
}
