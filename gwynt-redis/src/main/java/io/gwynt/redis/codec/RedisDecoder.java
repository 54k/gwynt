package io.gwynt.redis.codec;

import io.gwynt.core.codec.DecoderException;
import io.gwynt.core.codec.ReplayingDecoder;
import io.gwynt.core.pipeline.HandlerContext;
import io.gwynt.redis.codec.RedisDecoder.State;

import java.nio.ByteBuffer;
import java.util.List;

public class RedisDecoder extends ReplayingDecoder<State> {

    public static final char SIMPLE_STRING = '+';
    public static final char ERROR = '-';
    public static final char INTEGER = ':';
    public static final char BULK_STRING = '$';
    public static final char ARRAY = '*';

    public static final byte[] CRLF = {'\r', '\n'};

    public RedisDecoder() {
        super(State.READ_TYPE_TOKEN);
    }

    private static State state(char token) {
        switch (token) {
            case SIMPLE_STRING:
                return State.READ_SIMPLE_STRING;
            case ERROR:
                return State.READ_ERROR;
            case INTEGER:
                return State.READ_INTEGER;
            case BULK_STRING:
                return State.READ_BULK_STRING;
            case ARRAY:
                return State.READ_ARRAY;
            default:
                throw new DecoderException("unknown token: " + token);
        }
    }

    @Override
    protected void decode(HandlerContext context, ByteBuffer message, List<Object> out) {
        switch (state()) {
            case READ_TYPE_TOKEN:
                char token = message.getChar();
                checkpoint(state(token));
            case READ_SIMPLE_STRING:
            case READ_ERROR:
            case READ_INTEGER:
            case READ_BULK_STRING:
            case READ_ARRAY:
            case AWAIT_CRLF:

        }
    }

    public static enum State {
        READ_TYPE_TOKEN,
        READ_SIMPLE_STRING,
        READ_ERROR,
        READ_INTEGER,
        READ_BULK_STRING,
        READ_ARRAY,
        AWAIT_CRLF
    }
}
