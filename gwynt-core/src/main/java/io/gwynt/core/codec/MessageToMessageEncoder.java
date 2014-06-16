package io.gwynt.core.codec;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.pipeline.HandlerContext;

import java.util.ArrayList;
import java.util.List;

public abstract class MessageToMessageEncoder<O> extends AbstractHandler<Object, O> {

    private final List<Object> out = new ArrayList<>();

    @Override
    public void onMessageSent(HandlerContext context, O message, ChannelPromise channelPromise) {
        try {
            encode(context, message, out);
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncoderException(e);
        } finally {
            flushOut(context, channelPromise);
            out.clear();
        }
    }

    private void flushOut(HandlerContext context, ChannelPromise channelPromise) {
        if (out.isEmpty()) {
            throw new EncoderException(getClass().getSimpleName() + " out buffer is empty");
        }

        int sizeMinusOne = out.size() - 1;
        if (sizeMinusOne > 0) {
            for (int i = 0; i < sizeMinusOne; i++) {
                context.write(out.get(i));
            }
        }
        context.write(out.get(sizeMinusOne), channelPromise);
    }

    protected abstract void encode(HandlerContext context, O message, List<Object> out);
}
