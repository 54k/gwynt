package io.gwynt.core.codec;

import io.gwynt.core.pipeline.HandlerContext;

import java.nio.ByteBuffer;
import java.util.List;

public abstract class StateDecoder<S> extends ByteToMessageDecoder {

    private static final Signal SIGNAL = new Signal();

    private S state;
    private int checkpoint = -1;

    protected StateDecoder() {
        this(null);
    }

    protected StateDecoder(S state) {
        this.state = state;
    }

    protected static void signal() {
        throw SIGNAL;
    }

    protected S state(S state) {
        S oldState = this.state;
        this.state = state;
        return oldState;
    }

    protected S state() {
        return state;
    }

    protected void checkpoint() {
        checkpoint = internalBuffer().position();
    }

    protected void checkpoint(S state) {
        checkpoint();
        state(state);
    }

    @Override
    protected void callDecode(HandlerContext context, ByteBuffer in, List<Object> out) {
        try {
            while (in.hasRemaining()) {
                int oldPosition = checkpoint = in.position();
                int oldSize = out.size();
                S oldState = state;
                try {
                    decode(context, in, out);
                    if (context.isRemoved()) {
                        break;
                    }

                    if (oldSize == out.size()) {
                        if (oldPosition == in.position() && state == oldState) {
                            throw new DecoderException(getClass().getSimpleName() + "#decode did not decode anything");
                        }
                    }

                } catch (Signal s) {
                    if (checkpoint >= 0) {
                        in.position(checkpoint);
                    }
                    break;
                }

                if (oldPosition == in.position() && state == oldState) {
                    throw new DecoderException(getClass().getSimpleName() + "#decode did not decode anything");
                }
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new DecoderException(e);
        }
    }

    private final static class Signal extends Error {
        private Signal() {
            setStackTrace(new StackTraceElement[0]);
        }
    }
}
