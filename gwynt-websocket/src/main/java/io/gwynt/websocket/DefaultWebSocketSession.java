package io.gwynt.websocket;

import io.gwynt.core.IoSession;
import io.gwynt.websocket.protocol.CloseFrame;
import io.gwynt.websocket.protocol.Frame;
import io.gwynt.websocket.protocol.FrameUtils;
import io.gwynt.websocket.protocol.Handshake;

import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultWebSocketSession implements WebSocketSession {

    private AtomicBoolean handshakeCompleted = new AtomicBoolean(false);
    private Handshake handshake;

    private IoSession ioSession;
    private WebSocketIoHandler networkEventHandler;

    public DefaultWebSocketSession(IoSession ioSession, WebSocketIoHandler networkEventHandler) {
        this.ioSession = ioSession;
        this.networkEventHandler = networkEventHandler;
    }

    public boolean isHandshakeCompleted() {
        return handshakeCompleted.get();
    }

    public IoSession getIoSession() {
        return ioSession;
    }

    public void completeHandshake() {
        if (!isClosed()) {
            this.handshakeCompleted.set(true);
        }
    }

    public WebSocketIoHandler getNetworkEventHandler() {
        return networkEventHandler;
    }

    @Override
    public Handshake getHandshake() {
        return handshake;
    }

    public void setHandshake(Handshake handshake) {
        this.handshake = handshake;
    }

    @Override
    public void writeMessage(String message) {
        if (!ioSession.isPendingClose() && !ioSession.isClosed()) {
            sendFrame(true, FrameUtils.OPCODE_TEXT, message.getBytes());
        }
    }

    @Override
    public void writeMessage(byte[] message) {
        if (!ioSession.isPendingClose() && !ioSession.isClosed()) {
            sendFrame(true, FrameUtils.OPCODE_BINARY, message);
        }
    }

    @Override
    public void writePartialMessage(String message, boolean last) {
        if (!ioSession.isPendingClose() && !ioSession.isClosed()) {
            sendFrame(last, FrameUtils.OPCODE_TEXT, message.getBytes());
        }
    }

    @Override
    public void writePartialMessage(byte[] message, boolean last) {
        if (!ioSession.isPendingClose() && !ioSession.isClosed()) {
            sendFrame(last, FrameUtils.OPCODE_BINARY, message);
        }
    }

    @Override
    public void ping() {
        if (!ioSession.isPendingClose() && !ioSession.isClosed()) {
            sendFrame(true, FrameUtils.OPCODE_PING, new byte[0]);
        }
    }

    private void sendFrame(boolean fin, int opcode, byte[] data) {
        Frame frame = new Frame();
        frame.setFin(fin);
        frame.setPayload(data);
        frame.setOpcode(opcode);
        //        ioSession.write(frame);
    }

    @Override
    public void close() {
        close(CloseFrame.NORMAL);
    }

    @Override
    public void close(int reason) {
        CloseFrame frame = new CloseFrame(reason);
        close(frame);
    }

    public void close(CloseFrame frame) {
        if (!ioSession.isPendingClose()) {
            //            ioSession.write(frame);
            ioSession.close();
        }
    }

    @Override
    public boolean isPendingClose() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return ioSession.isClosed();
    }
}
