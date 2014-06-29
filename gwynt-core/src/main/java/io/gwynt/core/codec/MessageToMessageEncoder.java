package io.gwynt.core.codec;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.pipeline.HandlerContext;
import io.gwynt.core.util.ObjectMatcher;

import java.util.ArrayList;
import java.util.List;

public abstract class MessageToMessageEncoder<O> extends AbstractHandler<Object, O> {
    private static final ObjectMatcher<Object> DEFAULT_MATCHER = new ObjectMatcher<Object>() {
        @Override
        public boolean match(Object object) {
            return true;
        }
    };

    private final ObjectMatcher<? super O> matcher;
    private final List<Object> out = new ArrayList<>();

    protected MessageToMessageEncoder() {
        this(DEFAULT_MATCHER);
    }

    protected MessageToMessageEncoder(ObjectMatcher<? super O> matcher) {
        this.matcher = matcher;
    }

    @Override
    public void onMessageSent(HandlerContext context, O message, ChannelPromise channelPromise) {
        boolean discarded = false;
        try {
            if (!matcher.match(message)) {
                discarded = true;
                discardMessage(context, message);
            }
            encode(context, message, out);
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncoderException(e);
        } finally {
            if (!discarded) {
                flushOut(context, channelPromise);
                out.clear();
            }
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

    protected void discardMessage(HandlerContext context, O message) {
        // NO OP
    }

    protected abstract void encode(HandlerContext context, O message, List<Object> out);
}
