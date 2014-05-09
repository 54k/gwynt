package io.gwynt.websocket.exception;

import io.gwynt.websocket.protocol.CloseFrame;

public class ProtocolException extends RuntimeException {

    private CloseFrame closeFrame;

    public ProtocolException(CloseFrame closeFrame) {
        this.closeFrame = closeFrame;
    }

    public ProtocolException(String message) {
        super(message);
    }

    public CloseFrame getCloseFrame() {
        return closeFrame;
    }
}
