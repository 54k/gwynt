package io.gwynt.core;

import java.nio.ByteBuffer;

public interface ByteBufferPool {

    ByteBufferPool DEFAULT = new ArrayByteBufferPool();

    ByteBuffer acquire(int size, boolean direct);

    void release(ByteBuffer buffer);
}
