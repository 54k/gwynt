package io.gwynt.core.codec;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.pipeline.HandlerContext;
import io.gwynt.core.util.ObjectMatcher;

import java.util.ArrayList;
import java.util.List;

public abstract class MessageToMessageDecoder<I> extends AbstractHandler<I, Object> {

    private static final ObjectMatcher<Object> DEFAULT_MATCHER = new ObjectMatcher<Object>() {
        @Override
        public boolean match(Object object) {
            return true;
        }
    };

    private final ObjectMatcher<? super I> matcher;
    private final List<Object> out = new ArrayList<>();

    protected MessageToMessageDecoder() {
        this(DEFAULT_MATCHER);
    }

    protected MessageToMessageDecoder(ObjectMatcher<? super I> matcher) {
        this.matcher = matcher;
    }

    @Override
    public void onMessageReceived(HandlerContext context, I message) {
        boolean discarded = false;
        try {
            if (!matcher.match(message)) {
                discarded = true;
                discardMessage(context, message);
            }

            decode(context, message, out);
        } catch (DecoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new DecoderException(e);
        } finally {
            if (!discarded) {
                flushOut(context);
                out.clear();
            }
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

    protected void discardMessage(HandlerContext context, I message) {
        // NO OP
    }

    protected abstract void decode(HandlerContext context, I message, List<Object> out);
}
