package io.gwynt.core.util;

public final class Signal extends Error implements Constant<Signal> {

    private static final ConstantPool<Signal> pool = new ConstantPool<Signal>() {
        @Override
        protected Signal newConstant(int id, String name) {
            return new Signal(id, name);
        }
    };

    private final int id;
    private final String name;

    private Signal(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Signal valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return pool.valueOf(firstNameComponent, secondNameComponent);
    }

    public static Signal valueOf(String name) {
        return pool.valueOf(name);
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    public void expect(Signal signal) {
        if (this != signal) {
            throw new IllegalArgumentException("unexpected signal: " + signal);
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public int compareTo(Constant<Signal> o) {
        if (this == o) {
            return 0;
        }

        int returnCode = name.compareTo(o.name());
        if (returnCode != 0) {
            return returnCode;
        }

        return ((Integer) id).compareTo(o.id());
    }
}
