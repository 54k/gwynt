package io.gwynt.websocket;

import io.gwynt.core.IoSession;
import io.gwynt.core.IoSessionInitializer;
import io.gwynt.websocket.protocol.WebSocketHandshakeCodec;

public class WebSocketSessionInitializer extends IoSessionInitializer {

    @Override
    protected void initialize(IoSession session) {
        session.getPipeline().addFirst(new WebSocketHandshakeCodec(null));
    }
}
