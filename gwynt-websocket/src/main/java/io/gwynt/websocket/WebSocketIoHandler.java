package io.gwynt.websocket;

public interface WebSocketIoHandler {

    void onOpen(WebSocketSession connection);

    void onMessage(WebSocketSession connection, String message);

    void onMessage(WebSocketSession connection, byte[] message);

    void onPong(WebSocketSession connection);

    void onClose(WebSocketSession connection);

    void onError(WebSocketSession connection, Throwable e);
}
