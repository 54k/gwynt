package io.gwynt.core.buffer;

import java.nio.ByteBuffer;

public abstract class DynamicByteBuffer {

    private ByteBuffer buffer;
    private ByteBufferPool pool;

    protected DynamicByteBuffer(ByteBufferPool pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool");
        }
        this.pool = pool;
        buffer = newByteBuffer(pool);
    }

    public static DynamicByteBuffer allocate(ByteBufferPool pool, final int capacity) {
        return new DynamicByteBuffer(pool) {
            @Override
            protected ByteBuffer newByteBuffer(ByteBufferPool pool) {
                return pool.acquire(capacity, false);
            }
        };
    }

    public static DynamicByteBuffer allocateDirect(ByteBufferPool pool, final int capacity) {
        return new DynamicByteBuffer(pool) {
            @Override
            protected ByteBuffer newByteBuffer(ByteBufferPool pool) {
                return pool.acquire(capacity, true);
            }
        };
    }

    protected abstract ByteBuffer newByteBuffer(ByteBufferPool pool);

    private void ensureCapacity(int capacity) {
        checkReleased();
        if (buffer.remaining() < capacity) {
            int position = buffer.position();
            int newCapacity = position + capacity - buffer.remaining();

            ByteBuffer alloc = pool.acquire(newCapacity, buffer.isDirect());
            buffer.position(0);
            alloc.put(buffer);
            pool.release(buffer);
            alloc.position(position);
            buffer = alloc;
        }
    }

    public byte get() {
        checkReleased();
        return buffer.get();
    }

    public DynamicByteBuffer put(byte b) {
        ensureCapacity(1);
        buffer.put(b);
        return this;
    }

    public byte get(int index) {
        checkReleased();
        return buffer.get(index);
    }

    public DynamicByteBuffer put(int index, byte b) {
        ensureCapacity(1);
        buffer.put(index, b);
        return this;
    }

    public DynamicByteBuffer get(byte[] dst, int offset, int length) {
        checkReleased();
        buffer.get(dst, offset, length);
        return this;
    }

    public DynamicByteBuffer get(byte[] dst) {
        checkReleased();
        buffer.get(dst);
        return this;
    }

    public DynamicByteBuffer put(ByteBuffer src) {
        ensureCapacity(src.remaining());
        buffer.put(src);
        return this;
    }

    public DynamicByteBuffer put(byte[] src, int offset, int length) {
        ensureCapacity(length);
        buffer.put(src, offset, length);
        return this;
    }

    public char getChar() {
        checkReleased();
        return buffer.getChar();
    }

    public DynamicByteBuffer putChar(char value) {
        ensureCapacity(2);
        buffer.putChar(value);
        return this;
    }

    public char getChar(int index) {
        checkReleased();
        return buffer.getChar(index);
    }

    public DynamicByteBuffer putChar(int index, char value) {
        ensureCapacity(2);
        buffer.putChar(index, value);
        return this;
    }

    public short getShort() {
        checkReleased();
        return buffer.getShort();
    }

    public DynamicByteBuffer putShort(short value) {
        ensureCapacity(2);
        buffer.putShort(value);
        return this;
    }

    public short getShort(int index) {
        checkReleased();
        return buffer.getShort(index);
    }

    public DynamicByteBuffer putShort(int index, short value) {
        buffer.putShort(index, value);
        return this;
    }

    public int getInt() {
        checkReleased();
        return buffer.getInt();
    }

    public DynamicByteBuffer putInt(int value) {
        ensureCapacity(4);
        buffer.putInt(value);
        return this;
    }

    public int getInt(int index) {
        checkReleased();
        return buffer.getInt(index);
    }

    public DynamicByteBuffer putInt(int index, int value) {
        ensureCapacity(4);
        buffer.putInt(index, value);
        return this;
    }

    public long getLong() {
        checkReleased();
        return buffer.getLong();
    }

    public DynamicByteBuffer putLong(long value) {
        ensureCapacity(8);
        buffer.putLong(value);
        return this;
    }

    public long getLong(int index) {
        checkReleased();
        return buffer.getLong(index);
    }

    public DynamicByteBuffer putLong(int index, long value) {
        ensureCapacity(8);
        buffer.putLong(index, value);
        return this;
    }

    public float getFloat() {
        checkReleased();
        return buffer.getFloat();
    }

    public DynamicByteBuffer putFloat(float value) {
        ensureCapacity(4);
        buffer.putFloat(value);
        return this;
    }

    public float getFloat(int index) {
        checkReleased();
        return buffer.getFloat(index);
    }

    public DynamicByteBuffer putFloat(int index, float value) {
        ensureCapacity(4);
        buffer.putFloat(index, value);
        return this;
    }

    public double getDouble() {
        checkReleased();
        return buffer.getDouble();
    }

    public DynamicByteBuffer putDouble(double value) {
        ensureCapacity(8);
        buffer.putDouble(value);
        return this;
    }

    public double getDouble(int index) {
        checkReleased();
        return buffer.getDouble(index);
    }

    public DynamicByteBuffer putDouble(int index, double value) {
        ensureCapacity(8);
        buffer.putDouble(index, value);
        return this;
    }

    public DynamicByteBuffer flip() {
        checkReleased();
        buffer.flip();
        return this;
    }

    public int remaining() {
        checkReleased();
        return buffer.remaining();
    }

    public int position() {
        checkReleased();
        return buffer.position();
    }

    public DynamicByteBuffer duplicate() {
        checkReleased();
        return new DynamicByteBuffer(pool) {
            @Override
            protected ByteBuffer newByteBuffer(ByteBufferPool pool) {
                return buffer.duplicate();
            }
        };
    }

    public DynamicByteBuffer position(int newPosition) {
        checkReleased();
        buffer.position(newPosition);
        return this;
    }

    public DynamicByteBuffer compact() {
        checkReleased();
        buffer.compact();
        return this;
    }

    public void release() {
        pool.release(buffer);
        buffer = null;
        pool = null;
    }

    public ByteBuffer asByteBuffer() {
        checkReleased();
        return buffer;
    }

    private void checkReleased() {
        if (buffer == null) {
            throw new IllegalStateException("Buffer was released.");
        }
    }
}
