package io.gwynt.websocket;

public abstract class AbstractWebSocketIoHandler implements WebSocketIoHandler {

    @Override
    public void onOpen(WebSocketSession connection) {

    }

    @Override
    public void onMessage(WebSocketSession connection, String message) {

    }

    @Override
    public void onMessage(WebSocketSession connection, byte[] message) {

    }

    @Override
    public void onPong(WebSocketSession connection) {

    }

    @Override
    public void onClose(WebSocketSession connection) {

    }

    @Override
    public void onError(WebSocketSession connection, Throwable e) {

    }
}
