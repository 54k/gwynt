package io.gwynt.core.util;

public final class Arrays {

    public static final Object[] EMPTY_ARRAY = {};

    private Arrays() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] emptyArray() {
        return (T[]) EMPTY_ARRAY;
    }
}
