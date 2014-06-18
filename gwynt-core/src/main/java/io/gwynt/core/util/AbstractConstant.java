package io.gwynt.core.util;

public abstract class AbstractConstant<T extends AbstractConstant<T>> implements Constant<T> {

    private final int id;
    private final String name;

    protected AbstractConstant(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String name() {
        return name;
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
    public int compareTo(Constant<T> o) {
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
