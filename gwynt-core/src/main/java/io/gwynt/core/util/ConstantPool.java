package io.gwynt.core.util;

import java.util.HashMap;
import java.util.Map;

public abstract class ConstantPool<T extends Constant<T>> {

    private final Map<String, T> constants = new HashMap<>();
    private int nextId = 1;

    public T valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        if (firstNameComponent == null) {
            throw new NullPointerException("firstNameComponent");
        }
        if (secondNameComponent == null) {
            throw new NullPointerException("secondNameComponent");
        }

        return valueOf(firstNameComponent.getName() + '#' + secondNameComponent);
    }

    public T valueOf(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name");
        }

        synchronized (constants) {
            T c = constants.get(name);
            if (c == null) {
                c = newConstant(nextId, name);
                constants.put(name, c);
                nextId++;
            }
            return c;
        }
    }

    protected abstract T newConstant(int id, String name);
}
