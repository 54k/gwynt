package io.gwynt.core.group;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.concurrent.DefaultPromise;
import io.gwynt.core.concurrent.EventExecutor;
import io.gwynt.core.concurrent.Future;
import io.gwynt.core.concurrent.FutureListener;
import io.gwynt.core.concurrent.Promise;
import io.gwynt.core.exception.ChannelGroupException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class DefaultChannelGroupFuture extends DefaultPromise<Void> implements ChannelGroupFuture {

    private final ChannelGroup group;
    private Map<Channel, ChannelFuture> futures;
    private int successCount;
    private int failureCount;

    private final ChannelFutureListener childListener = new ChannelFutureListener() {
        @Override
        public void onComplete(ChannelFuture future) {
            synchronized (DefaultChannelGroupFuture.this) {
                if (future.isFailed()) {
                    failureCount++;
                } else {
                    successCount++;
                }
            }

            notify0();
        }
    };

    public DefaultChannelGroupFuture(ChannelGroup group, Collection<ChannelFuture> channelFutures) {
        this(null, group, channelFutures);
    }

    public DefaultChannelGroupFuture(EventExecutor eventExecutor, ChannelGroup group, Collection<ChannelFuture> futures) {
        super(eventExecutor);
        if (group == null) {
            throw new IllegalArgumentException("group");
        }
        if (futures == null) {
            throw new IllegalArgumentException("futures");
        }

        this.group = group;

        Map<Channel, ChannelFuture> futureMap = new LinkedHashMap<>();
        for (ChannelFuture f : futures) {
            futureMap.put(f.channel(), f);
        }
        this.futures = Collections.unmodifiableMap(futureMap);

        for (ChannelFuture f : futures) {
            f.addListener(childListener);
        }

        if (this.futures.isEmpty()) {
            setSuccess0();
        }
    }

    @Override
    public Iterator<ChannelFuture> iterator() {
        return futures.values().iterator();
    }

    @Override
    public ChannelGroup group() {
        return group;
    }

    private void setSuccess0() {
        super.setSuccess(null);
    }

    private void setFailure0(ChannelGroupException cause) {
        super.setFailure(cause);
    }

    private void notify0() {
        if (successCount + failureCount < futures.size()) {
            return;
        }
        assert successCount + failureCount == futures.size();
        if (failureCount > 0) {
            List<Entry<Channel, Throwable>> failed = new ArrayList<>(failureCount);
            for (ChannelFuture f : futures.values()) {
                if (f.isFailed()) {
                    failed.add(new DefaultEntry<>(f.channel(), f.getCause()));
                }
            }
            setFailure0(new ChannelGroupException(failed));
        } else {
            setSuccess0();
        }
    }

    @Override
    public Promise<Void> setFailure(Throwable cause) {
        throw new IllegalStateException();
    }

    @Override
    public boolean tryFailure(Throwable error) {
        return false;
    }

    @Override
    public Promise<Void> setSuccess(Void result) {
        throw new IllegalStateException();
    }

    @Override
    public boolean trySuccess(Void result) {
        return false;
    }

    @Override
    public ChannelGroupFuture addListener(FutureListener<? extends Future<? super Void>> futureListener) {
        super.addListener(futureListener);
        return this;
    }

    @Override
    public ChannelGroupFuture addListeners(FutureListener<? extends Future<? super Void>>... futureListeners) {
        super.addListeners(futureListeners);
        return this;
    }

    @Override
    public ChannelGroupFuture removeListener(FutureListener<? extends Future<? super Void>> futureListener) {
        super.removeListener(futureListener);
        return this;
    }

    @Override
    public ChannelGroupFuture removeListeners(FutureListener<? extends Future<? super Void>>... futureListeners) {
        super.removeListeners(futureListeners);
        return this;
    }

    @Override
    public ChannelGroupFuture await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public ChannelGroupFuture sync(long timeoutMillis) throws InterruptedException {
        super.sync(timeoutMillis);
        return this;
    }

    @Override
    public ChannelGroupFuture sync(long timeout, TimeUnit unit) throws InterruptedException {
        super.sync(timeout, unit);
        return this;
    }

    @Override
    public ChannelGroupFuture sync() throws InterruptedException {
        super.sync();
        return this;
    }

    private static final class DefaultEntry<K, V> implements Map.Entry<K, V> {

        private final K key;
        private final V value;

        public DefaultEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("read-only");
        }
    }
}
