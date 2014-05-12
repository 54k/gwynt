package io.gwynt.core.util;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class ByteBufferAllocator {

    private static final ByteBufferPool BYTE_BUFFER_POOL = new ByteBufferPool();

    public static ByteBuffer allocate(int capacity) {
        return BYTE_BUFFER_POOL.allocate(capacity);
    }

    public static void release(ByteBuffer byteBuffer) {
        BYTE_BUFFER_POOL.release(byteBuffer);
    }

    private static final class ByteBufferPool {

        private Deque<ByteBuffer> buffers = new ConcurrentLinkedDeque<>();

        public ByteBuffer allocate(int capacity) {
            ByteBuffer buffer = buffers.pollFirst();
            if (buffer == null || buffer.capacity() < capacity) {
                buffer = ByteBuffer.allocateDirect(capacity);
            }
            return buffer;
        }

        public void release(ByteBuffer byteBuffer) {
            byteBuffer.clear();
            buffers.addLast(byteBuffer);
        }
    }
}
