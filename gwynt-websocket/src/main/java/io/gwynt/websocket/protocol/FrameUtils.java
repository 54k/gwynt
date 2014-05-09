package io.gwynt.websocket.protocol;

public class FrameUtils {

    public static final int OPCODE_CONTINUATION = 0x0;
    public static final int OPCODE_TEXT = 0x1;
    public static final int OPCODE_BINARY = 0x2;
    public static final int OPCODE_CLOSE = 0x8;
    public static final int OPCODE_PING = 0x9;
    public static final int OPCODE_PONG = 0xA;

    private FrameUtils() {
    }

    public static boolean isControl(int opcode) {
        return opcode == OPCODE_CLOSE || opcode == OPCODE_PING || opcode == OPCODE_PONG;
    }

    public static byte[] longToBytes(long l, int len) {
        if (len > 8) {
            throw new IllegalArgumentException();
        }

        int shift = 0;
        byte[] result = new byte[len];
        for (int i = len - 1; i >= 0; i--) {
            result[i] = (byte) ((l >> shift) & 0xFF);
            shift += 8;
        }
        return result;
    }

    public static long bytesToLong(byte[] b, int start, int len) {
        if (len > 8) {
            throw new IllegalArgumentException();
        }

        int shift = 0;
        long result = 0;
        for (int i = start + len - 1; i >= start; i--) {
            result += (b[i] & 0xFF) << shift;
            shift += 8;
        }
        return result;
    }
}
