package io.gwynt.http;

import io.gwynt.core.codec.ReplayingDecoder;
import io.gwynt.core.pipeline.HandlerContext;
import io.gwynt.http.HttpRequestDecoder.State;

import java.nio.ByteBuffer;
import java.util.List;

public class HttpRequestDecoder extends ReplayingDecoder<State> {

    private final LineParser lineParser = new LineParser();

    private DefaultHttpRequest request;

    public HttpRequestDecoder() {
        super(State.SKIP_CONTROL_CHARACTERS);
    }

    private static void skipControlCharacters(ByteBuffer in) {
        while (in.hasRemaining()) {
            char c = (char) in.get();
            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
                in.position(in.position() - 1);
                break;
            }
        }
    }

    private static String[] splitInitialLine(String line) {
        return line.split("\\s");
    }

    private static DefaultHttpRequest newRequest(String[] initialLine) {
        return new DefaultHttpRequest(HttpMethod.valueOf(initialLine[0]), initialLine[1], HttpVersion.valueOf(initialLine[2]));
    }

    @Override
    protected void decode(HandlerContext context, ByteBuffer message, List<Object> out) {
        switch (state()) {
            case SKIP_CONTROL_CHARACTERS:
                skipControlCharacters(message);
                checkpoint(State.READ_INITIAL);
            case READ_INITIAL:
                String[] initialLine = splitInitialLine(lineParser.parse(message));
                if (initialLine.length < 3) {
                    state(State.READ_INITIAL);
                    replay();
                }
                request = newRequest(initialLine);
                checkpoint(State.READ_HEADERS);
            case READ_HEADERS:
        }
    }

    public static enum State {
        SKIP_CONTROL_CHARACTERS,
        READ_INITIAL,
        READ_HEADERS
    }

    private final class LineParser {

        private StringBuilder buffer = new StringBuilder();

        public String parse(ByteBuffer in) {
            while (in.hasRemaining()) {
                char c = (char) in.get();
                if (c == '\r') {
                    String result = buffer.toString();
                    buffer.delete(0, buffer.length());
                    return result;
                } else {
                    if (c == '\n') {
                        continue;
                    }
                    buffer.append(c);
                }
            }
            return buffer.toString();
        }
    }
}
