package io.gwynt.core;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ArrayByteBufferPool implements ByteBufferPool {

    private final static int DEFAULT_MIN_SIZE = 64;
    private final static int DEFAULT_STEP_SIZE = 1024;
    private final static int DEFAULT_MAX_SIZE = 65536;

    public static ByteBufferPool DEFAULT = new ArrayByteBufferPool();

    private final int minSize;
    private final Bucket[] direct;
    private final Bucket[] indirect;
    private final int increment;

    public ArrayByteBufferPool() {
        this(DEFAULT_MIN_SIZE, DEFAULT_STEP_SIZE, DEFAULT_MAX_SIZE);
    }

    public ArrayByteBufferPool(int minSize, int increment, int maxSize) {
        if (minSize >= increment) {
            throw new IllegalArgumentException("minSize >= increment");
        }
        if ((maxSize % increment) != 0 || increment >= maxSize) {
            throw new IllegalArgumentException("increment must be a divisor of maxSize");
        }

        this.minSize = minSize;
        this.increment = increment;

        int bucketLength = maxSize / increment;

        direct = new Bucket[bucketLength];
        indirect = new Bucket[bucketLength];

        int size = 0;
        for (int i = 0; i < direct.length; i++) {
            size += this.increment;
            direct[i] = new Bucket(size);
            indirect[i] = new Bucket(size);
        }
    }

    @Override
    public ByteBuffer acquire(int size, boolean direct) {
        Bucket bucket = bucketFor(size, direct);
        ByteBuffer buffer = bucket == null ? null : bucket.queue.poll();

        if (buffer == null) {
            int capacity = bucket == null ? size : bucket.size;
            buffer = direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
        }

        return buffer;
    }

    @Override
    public void release(ByteBuffer buffer) {
        if (buffer != null) {
            Bucket bucket = bucketFor(buffer.capacity(), buffer.isDirect());
            if (bucket != null) {
                buffer.clear();
                bucket.queue.offer(buffer);
            }
        }
    }

    @Override
    public void clear() {
        for (int i = 0; i < direct.length; i++) {
            direct[i].queue.clear();
            indirect[i].queue.clear();
        }
    }

    private Bucket bucketFor(int size, boolean direct) {
        if (size <= minSize) {
            return null;
        }
        int b = (size - 1) / increment;
        if (b >= this.direct.length) {
            return null;
        }

        return direct ? this.direct[b] : indirect[b];
    }

    public static class Bucket {
        public final int size;
        public final Queue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();

        Bucket(int size) {
            this.size = size;
        }
    }
}
