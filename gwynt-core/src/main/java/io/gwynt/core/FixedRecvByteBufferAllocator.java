package io.gwynt.core;

import java.nio.ByteBuffer;

public class FixedRecvByteBufferAllocator implements RecvByteBufferAllocator {

    public static final RecvByteBufferAllocator DEFAULT = new FixedRecvByteBufferAllocator();

    private final int recvSize;

    public FixedRecvByteBufferAllocator() {
        this(2048);
    }

    public FixedRecvByteBufferAllocator(int recvSize) {
        this.recvSize = recvSize;
    }

    @Override
    public Handle newHandle() {
        return new HandleImpl(recvSize);
    }

    private static final class HandleImpl implements Handle {

        private final int size;

        HandleImpl(int size) {
            this.size = size;
        }

        @Override
        public ByteBuffer allocate(ByteBufferPool byteBufferPool) {
            return byteBufferPool.acquire(size, true);
        }

        @Override
        public int guess() {
            return size;
        }

        @Override
        public void record(int bytesRead) {
        }
    }
}