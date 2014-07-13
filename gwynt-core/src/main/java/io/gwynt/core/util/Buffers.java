package io.gwynt.core.util;

import java.nio.ByteBuffer;

public final class Buffers {

    private Buffers() {
    }

    public static byte[] getBytes(ByteBuffer buffer) {
        int length = buffer.limit() - buffer.position();
        byte[] message = new byte[length];
        buffer.get(message, 0, length);
        return message;
    }
}
