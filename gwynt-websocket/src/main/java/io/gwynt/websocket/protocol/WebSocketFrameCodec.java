package io.gwynt.websocket.protocol;

import io.gwynt.core.AbstractIoHandler;
import io.gwynt.core.pipeline.IoHandlerContext;
import io.gwynt.websocket.DefaultWebSocketSession;
import io.gwynt.websocket.exception.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public abstract class WebSocketFrameCodec extends AbstractIoHandler<byte[], Frame> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketFrameCodec.class);
    private final byte[] mask = new byte[4];
    protected DefaultWebSocketSession webSocketConnection;
    private byte[] inputData;
    private int readPosition;
    private boolean continuationExpected = false;
    private ByteBuffer controlBuffer = ByteBuffer.allocate(125);
    // current frame
    private boolean fin = false;
    private byte opCode = 0;
    private byte[] payload;
    private int maskIndex = 0;
    private long payloadLength = 0;
    private long payloadWritten = 0;
    private State state = State.PROCESS_NEW_FRAME;
    private IoHandlerContext inboundContext;

    protected WebSocketFrameCodec(DefaultWebSocketSession webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    protected abstract boolean isServer();

    @Override
    public void onMessageSent(IoHandlerContext context, Frame message) {
        super.onMessageSent(context, message);
    }

    @Override
    public void onMessageReceived(IoHandlerContext context, byte[] message) {
        super.onMessageReceived(context, message);
    }

    @Override
    public void decode(IoHandlerContext context, byte[] message) {
        inboundContext = context;
        appendInputData(message);
        try {
            decode();
        } catch (ProtocolException e) {
            webSocketConnection.close(e.getCloseFrame());
            inboundContext.halt();
            logger.info("Protocol violation {}, closing session", e, webSocketConnection);
        }
    }

    @Override
    public void encode(IoHandlerContext context, Frame message) {
        context.proceed(encode(message));
    }

    public byte[] encode(Frame data) {
        byte[] payload = data.getPayload() != null ? data.getPayload() : new byte[0];
        int payloadLength = payload.length;

        int headerLength;
        byte[] header = new byte[10];

        if (data.isFin()) {
            header[0] |= 0x80;
        }

        header[0] |= data.getOpcode();

        if (payload.length <= 125) {
            header[1] = (byte) payloadLength;
            headerLength = 2;
        } else if (payload.length >= 126 && payload.length <= 65535) {
            header[1] = (byte) 126;
            byte[] longAsByte = FrameUtils.longToBytes(payloadLength, 2);
            System.arraycopy(longAsByte, 0, header, 2, longAsByte.length);
            headerLength = 4;
        } else {
            header[1] = (byte) 127;
            byte[] longAsByte = FrameUtils.longToBytes(payloadLength, 8);
            System.arraycopy(longAsByte, 0, header, 2, longAsByte.length);
            headerLength = 10;
        }

        int length = headerLength + payload.length;

        byte[] outputFrame = new byte[length];
        System.arraycopy(header, 0, outputFrame, 0, headerLength);
        System.arraycopy(payload, 0, outputFrame, headerLength, payloadLength);
        return outputFrame;
    }

    public void appendInputData(byte[] inputData) {
        if (this.inputData == null) {
            this.inputData = inputData;
        } else {
            int remainingData = remainingData();
            if (remainingData > 0) {
                byte[] newBuffer = new byte[remainingData + inputData.length];
                System.arraycopy(this.inputData, readPosition, newBuffer, 0, remainingData);
                System.arraycopy(inputData, 0, newBuffer, remainingData, inputData.length);
                this.inputData = newBuffer;
                readPosition = 0;
            } else {
                this.inputData = inputData;
            }
        }
    }

    public void decode() {
        if (state == State.PROCESS_NEW_FRAME) {
            processInitialHeader();
        }

        if (state == State.PROCESS_REMAINING_HEADER) {
            processRemainingHeader();
        }

        if (state == State.PROCESS_PAYLOAD) {
            if (payload == null) {
                payload = new byte[(int) payloadLength];
            }
            processPayload();
        }
    }

    private void processInitialHeader() {
        int b = inputData[readPosition++];
        fin = (b & 0x80) > 0;
        int rsv = (b & 0x70) >>> 4;

        if (rsv != 0) {
            throw new ProtocolException(new CloseFrame(CloseFrame.PROTOCOL_ERROR, "Invalid RSV bits"));
        }

        opCode = (byte) (b & 0x0F);
        if (FrameUtils.isControl(opCode)) {
            if (!fin) {
                throw new ProtocolException(new CloseFrame(CloseFrame.PROTOCOL_ERROR, "Fragmented control frame"));
            }
        } else {
            if (continuationExpected) {
                if (opCode != FrameUtils.OPCODE_CONTINUATION) {
                    throw new ProtocolException(new CloseFrame(CloseFrame.PROTOCOL_ERROR, "Continuation frame expected"));
                }
            } else {
                if (opCode != FrameUtils.OPCODE_BINARY && opCode != FrameUtils.OPCODE_TEXT) {
                    throw new ProtocolException(new CloseFrame(CloseFrame.PROTOCOL_ERROR, "Invalid opcode " + opCode));
                }
            }
            continuationExpected = !fin;
        }
        b = inputData[readPosition++];

        if ((b & 0x80) == 0 && isServer()) {
            throw new ProtocolException(new CloseFrame(CloseFrame.PROTOCOL_ERROR, "Unmasked client frame"));
        }

        payloadLength = b & 0x7F;
        state = State.PROCESS_REMAINING_HEADER;
    }

    private void processRemainingHeader() {
        int headerLength;
        if (isServer()) {
            headerLength = 4;
        } else {
            headerLength = 0;
        }

        if (payloadLength == 126) {
            headerLength += 2;
        } else if (payloadLength == 127) {
            headerLength += 8;
        }

        if (!hasEnoughData(headerLength)) {
            return;
        }

        if (payloadLength == 126) {
            payloadLength = FrameUtils.bytesToLong(inputData, readPosition, 2);
            readPosition += 2;
        } else if (payloadLength == 127) {
            payloadLength = FrameUtils.bytesToLong(inputData, readPosition, 8);
            readPosition += 8;
        }

        if (FrameUtils.isControl(opCode)) {
            if (payloadLength > 125) {
                throw new ProtocolException(new CloseFrame(CloseFrame.PROTOCOL_ERROR, "Control frame payload too big " + payloadLength));
            }
            if (!fin) {
                throw new ProtocolException(new CloseFrame(CloseFrame.PROTOCOL_ERROR, "Fragmented control frame"));
            }
        }

        if (isServer()) {
            System.arraycopy(inputData, readPosition, mask, 0, 4);
            readPosition += 4;
        }
        state = State.PROCESS_PAYLOAD;
    }

    private void processPayload() {
        if (FrameUtils.isControl(opCode)) {
            processControlFrame();
        } else {
            processDataFrame();
        }
    }

    private void processControlFrame() {
        if (!appendPayloadToMessage(controlBuffer)) {
            return;
        }
        controlBuffer.flip();
        if (opCode == FrameUtils.OPCODE_CLOSE) {
            int code = CloseFrame.NORMAL;
            if (controlBuffer.remaining() == 1) {
                controlBuffer.clear();
                // Payload must be zero or 2+ bytes long
                throw new ProtocolException(new CloseFrame(CloseFrame.PROTOCOL_ERROR, "1 byte close code"));
            }
            if (controlBuffer.remaining() > 1) {
                code = controlBuffer.getShort();
            }
            inboundContext.proceed(new CloseFrame(code, controlBuffer.asCharBuffer().toString()));
        } else if (opCode == FrameUtils.OPCODE_PING) {
            Frame pongFrame = new Frame(FrameUtils.OPCODE_PONG, true, controlBufferToByteArray());
            inboundContext.getIoSession().write(pongFrame);
            inboundContext.halt();
        } else if (opCode == FrameUtils.OPCODE_PONG) {
            Frame pongFrame = new Frame(FrameUtils.OPCODE_PONG, true, controlBufferToByteArray());
            inboundContext.proceed(pongFrame);
        } else {
            controlBuffer.clear();
            throw new ProtocolException(new CloseFrame(CloseFrame.PROTOCOL_ERROR, "Invalid opcode " + opCode));
        }
        controlBuffer.clear();
        newFrame();
    }

    private byte[] controlBufferToByteArray() {
        byte[] result = new byte[controlBuffer.limit()];
        for (int i = 0; controlBuffer.hasRemaining(); i++) {
            result[i] = controlBuffer.get();
        }
        return result;
    }

    private void processDataFrame() {
        if (!appendPayloadToMessage(ByteBuffer.wrap(payload))) {
            if (!hasEnoughData(payloadLength - payloadWritten)) {
                inboundContext.halt();
                return;
            }
        }

        Frame dataFrame = new Frame();
        dataFrame.setPayload(payload);
        dataFrame.setFin(fin);
        dataFrame.setOpcode(opCode);

        if (continuationExpected) {
            inboundContext.proceed(dataFrame);
            newFrame();
        } else {
            inboundContext.proceed(dataFrame);
            newMessage();
        }
    }

    private boolean appendPayloadToMessage(ByteBuffer payload) {
        if (isServer()) {
            while (payloadWritten < payloadLength && hasEnoughData(payloadLength - payloadWritten) &&
                    payload.hasRemaining()) {
                byte b = (byte) ((inputData[readPosition] ^ mask[maskIndex]) & 0xFF);
                maskIndex++;
                if (maskIndex == 4) {
                    maskIndex = 0;
                }
                readPosition++;
                payloadWritten++;
                payload.put(b);
            }
            return payloadWritten == payloadLength;
        } else {
            long toWrite = Math.min(payloadLength - payloadWritten, remainingData());
            toWrite = Math.min(toWrite, payload.remaining());

            payload.put(inputData, readPosition, (int) toWrite);
            readPosition += toWrite;
            payloadWritten += toWrite;
            return payloadWritten == payloadLength;
        }
    }

    private boolean hasEnoughData(long requiredData) {
        return remainingData() >= requiredData;
    }

    private int remainingData() {
        return inputData.length - readPosition;
    }

    private void newMessage() {
        continuationExpected = false;
        newFrame();
    }

    private void newFrame() {
        readPosition = 0;
        maskIndex = 0;

        if (!continuationExpected) {
            payloadWritten = 0;
            inputData = null;
            payload = null;
        }

        state = State.PROCESS_NEW_FRAME;
    }

    private static enum State {
        PROCESS_NEW_FRAME, PROCESS_REMAINING_HEADER, PROCESS_PAYLOAD
    }
}
