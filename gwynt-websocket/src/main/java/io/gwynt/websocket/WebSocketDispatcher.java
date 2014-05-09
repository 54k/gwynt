package io.gwynt.websocket;

import org.apache.commons.codec.binary.StringUtils;
import io.gwynt.core.IoSession;
import io.gwynt.core.IoHandler;
import org.gwynt.core.filterchain.Decoder;
import io.gwynt.websocket.protocol.Frame;
import io.gwynt.websocket.protocol.FrameUtils;
import io.gwynt.websocket.protocol.WebSocketHandshakeCodec;

public class WebSocketDispatcher implements IoHandler<Frame> {

    private WebSocketIoHandler webSocketIoHandler;
    private Decoder webSocketSessionInitializer;

    public WebSocketDispatcher(WebSocketIoHandler webSocketIoHandler) {
        this.webSocketIoHandler = webSocketIoHandler;
        webSocketSessionInitializer = new WebSocketHandshakeCodec(webSocketIoHandler);
    }

    @Override
    public void onOpen(final IoSession ioSession) {
        ioSession.attach(new DefaultWebSocketSession(ioSession, webSocketIoHandler));
        ioSession.getPipeline().addCodec(webSocketSessionInitializer);
    }

    @Override
    public void onMessage(IoSession ioSession, Frame message) {
        switch (message.getOpcode()) {
            case FrameUtils.OPCODE_TEXT:
                webSocketIoHandler.onMessage(getWebSocketConnection(ioSession), StringUtils.newStringUtf8(message.getPayload()));
                break;
            case FrameUtils.OPCODE_BINARY:
                webSocketIoHandler.onMessage(getWebSocketConnection(ioSession), message.getPayload());
                break;
            case FrameUtils.OPCODE_CLOSE:
                ioSession.close();
                break;
            case FrameUtils.OPCODE_CONTINUATION:
                break;
            case FrameUtils.OPCODE_PONG:
                webSocketIoHandler.onPong(getWebSocketConnection(ioSession));
                break;
            default:
                // should never happen
                throw new IllegalStateException("Unknown opcode");
        }
    }

    @Override
    public void onClose(IoSession ioSession) {
        DefaultWebSocketSession defaultWebSocketConnection = (DefaultWebSocketSession) ioSession.attach(null);
        if (defaultWebSocketConnection.isHandshakeCompleted()) {
            webSocketIoHandler.onClose(defaultWebSocketConnection);
        }
    }

    @Override
    public void onError(IoSession ioSession, Throwable e) {
        webSocketIoHandler.onError(getWebSocketConnection(ioSession), e);
    }

    private DefaultWebSocketSession getWebSocketConnection(IoSession ioSession) {
        return (DefaultWebSocketSession) ioSession.attachment();
    }
}
