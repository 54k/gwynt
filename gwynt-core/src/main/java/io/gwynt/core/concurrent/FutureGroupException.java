package io.gwynt.core.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class FutureGroupException extends RuntimeException implements Iterable<Entry<Future, Throwable>> {

    private final Collection<Entry<Future, Throwable>> failed;

    public FutureGroupException(Collection<Map.Entry<Future, Throwable>> causes) {
        if (causes == null) {
            throw new NullPointerException("causes");
        }
        if (causes.isEmpty()) {
            throw new IllegalArgumentException("causes must be non empty");
        }
        failed = Collections.unmodifiableCollection(causes);
    }

    @Override
    public Iterator<Entry<Future, Throwable>> iterator() {
        return failed.iterator();
    }
}
