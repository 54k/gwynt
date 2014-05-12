package io.gwynt.core.transport;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ByteBufferAllocator {

    private static final ByteBufferPool BYTE_BUFFER_POOL = new ByteBufferPool();

    public static ByteBuffer allocate(int capacity) {
        return BYTE_BUFFER_POOL.allocate(capacity);
    }

    public static void release(ByteBuffer byteBuffer) {
        BYTE_BUFFER_POOL.release(byteBuffer);
    }

    private static class ByteBufferPool {

        private Queue<ByteBuffer> byteBufferQueue = new ConcurrentLinkedQueue<>();

        public ByteBuffer allocate(int capacity) {
            ByteBuffer buffer = byteBufferQueue.poll();
            if (buffer == null) {
                buffer = ByteBuffer.allocateDirect(capacity);
            }
            return buffer;
        }

        public void release(ByteBuffer byteBuffer) {
            byteBuffer.clear();
            byteBufferQueue.add(byteBuffer);
        }
    }
}
