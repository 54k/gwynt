package io.gwynt.core.codec;

public class EncoderException extends RuntimeException {

    public EncoderException(Throwable cause) {
        super(cause);
    }

    public EncoderException(String message) {
        super(message);
    }
}
