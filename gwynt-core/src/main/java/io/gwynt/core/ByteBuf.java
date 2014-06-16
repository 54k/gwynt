package io.gwynt.core;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public abstract class ByteBuf {

    public ByteBuf get(byte[] dst, int offset, int length) {
        return null;
    }

    public ByteBuf get(byte[] dst) {
        return null;
    }

    public ByteBuf put(ByteBuffer src) {
        return null;
    }

    public ByteBuf put(byte[] src, int offset, int length) {
        return null;
    }

    public ByteBuf slice() {
        return null;
    }

    public ByteBuf duplicate() {
        return null;
    }

    public ByteBuf asReadOnlyBuffer() {
        return null;
    }

    public byte get() {
        return 0;
    }

    public ByteBuf put(byte b) {
        return null;
    }

    public byte get(int index) {
        return 0;
    }

    public ByteBuf put(int index, byte b) {
        return null;
    }

    public ByteBuf compact() {
        return null;
    }

    public boolean isDirect() {
        return false;
    }

    public char getChar() {
        return 0;
    }

    public ByteBuf putChar(char value) {
        return null;
    }

    public char getChar(int index) {
        return 0;
    }

    public ByteBuf putChar(int index, char value) {
        return null;
    }

    public ByteBuf asCharBuffer() {
        return null;
    }

    public short getShort() {
        return 0;
    }

    public ByteBuf putShort(short value) {
        return null;
    }

    public short getShort(int index) {
        return 0;
    }

    public ByteBuf putShort(int index, short value) {
        return null;
    }

    public ShortBuffer asShortBuffer() {
        return null;
    }

    public int getInt() {
        return 0;
    }

    public ByteBuf putInt(int value) {
        return null;
    }

    public int getInt(int index) {
        return 0;
    }

    public ByteBuf putInt(int index, int value) {
        return null;
    }

    public IntBuffer asIntBuffer() {
        return null;
    }

    public long getLong() {
        return 0;
    }

    public ByteBuf putLong(long value) {
        return null;
    }

    public long getLong(int index) {
        return 0;
    }

    public ByteBuf putLong(int index, long value) {
        return null;
    }

    public LongBuffer asLongBuffer() {
        return null;
    }

    public float getFloat() {
        return 0;
    }

    public ByteBuf putFloat(float value) {
        return null;
    }

    public float getFloat(int index) {
        return 0;
    }

    public ByteBuf putFloat(int index, float value) {
        return null;
    }

    public FloatBuffer asFloatBuffer() {
        return null;
    }

    public double getDouble() {
        return 0;
    }

    public ByteBuf putDouble(double value) {
        return null;
    }

    public double getDouble(int index) {
        return 0;
    }

    public ByteBuf putDouble(int index, double value) {
        return null;
    }

    public DoubleBuffer asDoubleBuffer() {
        return null;
    }

    public boolean isReadOnly() {
        return false;
    }
}
