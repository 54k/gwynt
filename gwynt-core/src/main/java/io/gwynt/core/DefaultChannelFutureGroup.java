package io.gwynt.core;

import io.gwynt.core.concurrent.AbstractFutureGroup;
import io.gwynt.core.concurrent.EventExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DefaultChannelFutureGroup extends AbstractFutureGroup<Void, ChannelFutureGroup> implements ChannelFutureGroup {

    private final ChannelFutureListener childListener = new ChannelFutureListener() {
        @Override
        public void onComplete(ChannelFuture future) {
            count(future);
        }
    };
    private Map<Channel, ChannelFuture> futures;

    public DefaultChannelFutureGroup(Collection<ChannelFuture> channelFutures) {
        this(null, channelFutures);
    }

    public DefaultChannelFutureGroup(EventExecutor eventExecutor, Collection<ChannelFuture> futures) {
        super(eventExecutor);
        if (futures == null) {
            throw new IllegalArgumentException("futures");
        }

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
    protected void notify0() {
        if (successCount() + failureCount() < futures.size()) {
            return;
        }
        assert successCount() + failureCount() == futures.size();
        if (failureCount() > 0) {
            List<Entry<Channel, Throwable>> failed = new ArrayList<>(failureCount());
            for (ChannelFuture f : futures.values()) {
                if (!f.isSuccess()) {
                    failed.add(new DefaultEntry<>(f.channel(), f.getCause()));
                }
            }
            setFailure0(new ChannelFutureGroupException(failed));
        } else {
            setSuccess0();
        }
    }
}
