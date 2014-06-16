package io.gwynt.core.codec;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.pipeline.HandlerContext;

import java.util.List;

public abstract class MessageToMessageCodec<I, O> extends AbstractHandler<I, O> {

    private final Decoder decoder;
    private final Encoder encoder;

    protected MessageToMessageCodec() {
        decoder = new Decoder();
        encoder = new Encoder();
    }

    @Override
    public void onMessageReceived(HandlerContext context, I message) {
        decoder.onMessageReceived(context, message);
    }

    @Override
    public void onMessageSent(HandlerContext context, O message, ChannelPromise channelPromise) {
        encoder.onMessageSent(context, message, channelPromise);
    }

    protected abstract void decode(HandlerContext context, I message, List<Object> out);

    protected abstract void encode(HandlerContext context, O message, List<Object> out);

    private final class Decoder extends MessageToMessageDecoder<I> {
        @Override
        protected void decode(HandlerContext context, I message, List<Object> out) {
            MessageToMessageCodec.this.decode(context, message, out);
        }
    }

    private final class Encoder extends MessageToMessageEncoder<O> {
        @Override
        protected void encode(HandlerContext context, O message, List<Object> out) {
            MessageToMessageCodec.this.encode(context, message, out);
        }
    }
}
