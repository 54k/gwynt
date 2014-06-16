package io.gwynt.core.codec;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.pipeline.HandlerContext;

import java.util.ArrayList;
import java.util.List;

public abstract class MessageToMessageDecoder<I> extends AbstractHandler<I, Object> {

    private final List<Object> out = new ArrayList<>();

    @Override
    public void onMessageReceived(HandlerContext context, I message) {
        try {
            decode(context, message, out);
        } catch (DecoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new DecoderException(e);
        } finally {
            flushOut(context);
            out.clear();
        }
    }

    private void flushOut(HandlerContext context) {
        if (out.isEmpty()) {
            throw new EncoderException(getClass().getSimpleName() + " out buffer is empty");
        }
        for (Object m : out) {
            context.fireMessageReceived(m);
        }
    }

    protected abstract void decode(HandlerContext context, I message, List<Object> out);
}
