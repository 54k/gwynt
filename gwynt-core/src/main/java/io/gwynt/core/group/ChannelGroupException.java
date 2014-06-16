package io.gwynt.core.group;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelException;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ChannelGroupException extends ChannelException implements Iterable<Map.Entry<Channel, Throwable>> {

    private final Collection<Entry<Channel, Throwable>> failed;

    public ChannelGroupException(Collection<Map.Entry<Channel, Throwable>> causes) {
        if (causes == null) {
            throw new NullPointerException("causes");
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
