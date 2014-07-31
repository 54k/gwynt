package io.gwynt.core.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public final class DefaultFutureGroup<V> extends AbstractFutureGroup<V, FutureGroup<V>> implements FutureGroup<V> {

    @SuppressWarnings("FieldCanBeLocal")
    private final FutureListener<Future<V>> futureListener = new FutureListener<Future<V>>() {
        @Override
        public void onComplete(Future future) {
            count(future);
        }
    };
    private Set<Future<V>> futures;

    public DefaultFutureGroup(Collection<? extends Future<V>> futures) {
        this(null, futures);
    }

    public DefaultFutureGroup(EventExecutor eventExecutor, Collection<? extends Future<V>> futures) {
        super(eventExecutor);
        if (futures == null) {
            throw new IllegalArgumentException("futures");
        }

        Set<Future<V>> futureSet = new LinkedHashSet<>(futures.size());
        for (Future<V> f : futures) {
            futureSet.add(f);
        }
        this.futures = Collections.unmodifiableSet(futureSet);

        for (Future<V> f : futures) {
            f.addListener(futureListener);
        }

        if (this.futures.isEmpty()) {
            setSuccess0();
        }
    }

    @Override
    protected void notify0() {
        if (successCount() + failureCount() < futures.size()) {
            return;
        }
        assert successCount() + failureCount() == futures.size();
        if (failureCount() > 0) {
            List<Entry<Future, Throwable>> failed = new ArrayList<>(failureCount());
            for (Future f : futures) {
                if (!f.isSuccess()) {
                    failed.add(new DefaultEntry<>(f, f.getCause()));
                }
            }
            setFailure0(new FutureGroupException(failed));
        } else {
            setSuccess0();
        }
    }

    @Override
    public Iterator<Future<V>> iterator() {
        return futures.iterator();
    }
}
