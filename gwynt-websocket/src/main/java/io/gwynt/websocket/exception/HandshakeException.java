package io.gwynt.websocket.exception;

public class HandshakeException extends RuntimeException {

    public HandshakeException(String message) {
        super(message);
    }

    public HandshakeException(Throwable cause) {
        super(cause);
    }
}
