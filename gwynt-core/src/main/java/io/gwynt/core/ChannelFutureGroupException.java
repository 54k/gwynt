package io.gwynt.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ChannelFutureGroupException extends ChannelException implements Iterable<Map.Entry<Channel, Throwable>> {

    private final Collection<Entry<Channel, Throwable>> failed;

    public ChannelFutureGroupException(Collection<Map.Entry<Channel, Throwable>> causes) {
        if (causes == null) {
            throw new IllegalArgumentException("causes");
        }
        if (causes.isEmpty()) {
            throw new IllegalArgumentException("causes must be non empty");
        }
        failed = Collections.unmodifiableCollection(causes);
    }

    @Override
    public Iterator<Entry<Channel, Throwable>> iterator() {
        return failed.iterator();
    }
}
