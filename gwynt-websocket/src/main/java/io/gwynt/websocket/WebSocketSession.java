package io.gwynt.websocket;

import io.gwynt.websocket.protocol.Handshake;

public interface WebSocketSession {

    Handshake getHandshake();

    void writeMessage(String message);

    void writeMessage(byte[] message);

    void writePartialMessage(String message, boolean last);

    void writePartialMessage(byte[] message, boolean last);

    void ping();

    void close();

    void close(int reason);

    boolean isPendingClose();

    boolean isClosed();
}
