package io.gwynt.core.buffer;

import java.nio.ByteBuffer;

public abstract class AbstractDynamicByteBuffer {

    private ByteBuffer buffer;
    private ByteBufferPool pool;

    protected AbstractDynamicByteBuffer(ByteBufferPool pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool");
        }
        this.pool = pool;
        buffer = newByteBuffer(pool);
    }

    protected abstract ByteBuffer newByteBuffer(ByteBufferPool pool);

    private void ensureCapacity(int capacity) {
        if (buffer.remaining() < capacity) {
        }
    }

    public byte get() {
        return 0;
    }

    public AbstractDynamicByteBuffer put(byte b) {
        return this;
    }

    public byte get(int index) {
        return 0;
    }

    public AbstractDynamicByteBuffer put(int index, byte b) {
        return this;
    }

    public AbstractDynamicByteBuffer get(byte[] dst, int offset, int length) {
        return this;
    }

    public AbstractDynamicByteBuffer get(byte[] dst) {
        return this;
    }

    public AbstractDynamicByteBuffer put(ByteBuffer src) {
        return this;
    }

    public AbstractDynamicByteBuffer put(byte[] src, int offset, int length) {
        return this;
    }

    public char getChar() {
        return 0;
    }

    public AbstractDynamicByteBuffer putChar(char value) {
        return this;
    }

    public char getChar(int index) {
        return 0;
    }

    public AbstractDynamicByteBuffer putChar(int index, char value) {
        return this;
    }

    public short getShort() {
        return 0;
    }

    public AbstractDynamicByteBuffer putShort(short value) {
        return this;
    }

    public short getShort(int index) {
        return 0;
    }

    public AbstractDynamicByteBuffer putShort(int index, short value) {
        return this;
    }

    public AbstractDynamicByteBuffer asShortBuffer() {
        return this;
    }

    public int getInt() {
        return 0;
    }

    public AbstractDynamicByteBuffer putInt(int value) {
        return null;
    }

    public int getInt(int index) {
        return 0;
    }

    public AbstractDynamicByteBuffer putInt(int index, int value) {
        return null;
    }

    public long getLong() {
        return 0;
    }

    public AbstractDynamicByteBuffer putLong(long value) {
        return null;
    }

    public long getLong(int index) {
        return 0;
    }

    public AbstractDynamicByteBuffer putLong(int index, long value) {
        return null;
    }

    public AbstractDynamicByteBuffer asLongBuffer() {
        return null;
    }

    public float getFloat() {
        return 0;
    }

    public AbstractDynamicByteBuffer putFloat(float value) {
        return null;
    }

    public float getFloat(int index) {
        return 0;
    }

    public AbstractDynamicByteBuffer putFloat(int index, float value) {
        return null;
    }

    public double getDouble() {
        return 0;
    }

    public AbstractDynamicByteBuffer putDouble(double value) {
        return null;
    }

    public double getDouble(int index) {
        return 0;
    }

    public AbstractDynamicByteBuffer putDouble(int index, double value) {
        return null;
    }
}
