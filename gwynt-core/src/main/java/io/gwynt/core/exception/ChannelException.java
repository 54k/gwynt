package io.gwynt.core.exception;

public class ChannelException extends RuntimeException {

    public ChannelException(String message) {
        super(message);
    }

    public ChannelException(Throwable cause) {
        super(cause);
    }
}
