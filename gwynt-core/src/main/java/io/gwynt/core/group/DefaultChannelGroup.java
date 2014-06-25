package io.gwynt.core.group;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ServerChannel;
import io.gwynt.core.concurrent.EventExecutor;
import io.gwynt.core.util.CombinedIterator;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultChannelGroup extends AbstractSet<Channel> implements ChannelGroup {
    private static final Object[] EMPTY_ARRAY = {};

    private static final AtomicLong sequence = new AtomicLong(0);
    private final ChannelFutureListener remover = new ChannelFutureListener() {
        @Override
        public void onComplete(ChannelFuture future) {
            remove(future.channel());
        }
    };

    private EventExecutor eventExecutor;
    private String name;
    private Set<Channel> nonServerChannels = Collections.newSetFromMap(new ConcurrentHashMap<Channel, Boolean>());
    private Set<Channel> serverChannels = Collections.newSetFromMap(new ConcurrentHashMap<Channel, Boolean>());

    public DefaultChannelGroup() {
        this((EventExecutor) null);
    }

    public DefaultChannelGroup(String name) {
        this(name, null);
    }

    public DefaultChannelGroup(EventExecutor eventExecutor) {
        this(generateName(), eventExecutor);
    }

    public DefaultChannelGroup(String name, EventExecutor eventExecutor) {
        this.name = name;
        this.eventExecutor = eventExecutor;
    }

    private static String generateName() {
        return "channel-group-" + DefaultChannelGroup.sequence.incrementAndGet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Channel> iterator() {
        return new CombinedIterator<>(nonServerChannels.iterator(), serverChannels.iterator());
    }

    @Override
    public int size() {
        return nonServerChannels.size() + serverChannels.size();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public <T> T accept(ChannelGroupVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public ChannelGroupFuture read(ChannelMatcher channelMatcher) {
        if (channelMatcher == null) {
            throw new IllegalArgumentException("channelMatcher");
        }

        ChannelMatcher matcher = ChannelMatchers.compose(ChannelMatchers.isNotServerChannel(), channelMatcher);
        List<ChannelFuture> futures = new ArrayList<>();
        for (Channel ch : this) {
            if (matcher.match(ch)) {
                futures.add(ch.read());
            }
        }
        return new DefaultChannelGroupFuture(this, futures);
    }

    @Override
    public ChannelGroupFuture read() {
        return read(ChannelMatchers.all());
    }

    @Override
    public ChannelGroupFuture write(Object message, ChannelMatcher channelMatcher) {
        if (message == null) {
            throw new IllegalArgumentException("message");
        }
        if (channelMatcher == null) {
            throw new IllegalArgumentException("channelMatcher");
        }

        ChannelMatcher matcher = ChannelMatchers.compose(ChannelMatchers.isNotServerChannel(), channelMatcher);
        List<ChannelFuture> futures = new ArrayList<>();
        for (Channel ch : this) {
            if (matcher.match(ch)) {
                futures.add(ch.write(message));
            }
        }
        return new DefaultChannelGroupFuture(eventExecutor, this, futures);
    }

    @Override
    public ChannelGroupFuture write(Object message) {
        return write(message, ChannelMatchers.all());
    }

    @Override
    public ChannelGroupFuture close(ChannelMatcher channelMatcher) {
        if (channelMatcher == null) {
            throw new IllegalArgumentException("channelMatcher");
        }

        List<ChannelFuture> futures = new ArrayList<>();
        for (Channel ch : this) {
            if (channelMatcher.match(ch)) {
                futures.add(ch.close());
            }
        }
        return new DefaultChannelGroupFuture(eventExecutor, this, futures);
    }

    @Override
    public ChannelGroupFuture close() {
        return close(ChannelMatchers.all());
    }

    @Override
    public ChannelGroupFuture unregister(ChannelMatcher channelMatcher) {
        if (channelMatcher == null) {
            throw new IllegalArgumentException("channelMatcher");
        }
        List<ChannelFuture> futures = new ArrayList<>();
        for (Channel ch : this) {
            if (channelMatcher.match(ch)) {
                futures.add(ch.unregister());
            }
        }
        return new DefaultChannelGroupFuture(eventExecutor, this, futures);
    }

    @Override
    public ChannelGroupFuture unregister() {
        return unregister(ChannelMatchers.all());
    }

    @Override
    public ChannelGroup newGroup(ChannelMatcher channelMatcher) {
        return newGroup(channelMatcher, generateName(), eventExecutor);
    }

    @Override
    public ChannelGroup newGroup(ChannelMatcher channelMatcher, String name) {
        return newGroup(channelMatcher, name, eventExecutor);
    }

    @Override
    public ChannelGroup newGroup(ChannelMatcher channelMatcher, EventExecutor eventExecutor) {
        return newGroup(channelMatcher, generateName(), eventExecutor);
    }

    @Override
    public ChannelGroup newGroup(ChannelMatcher channelMatcher, String name, EventExecutor eventExecutor) {
        if (channelMatcher == null) {
            throw new IllegalArgumentException("channelMatcher");
        }
        DefaultChannelGroup newGroup = new DefaultChannelGroup(name, eventExecutor);
        return fillGroup(newGroup, channelMatcher);
    }

    private ChannelGroup fillGroup(ChannelGroup newGroup, ChannelMatcher channelMatcher) {
        for (Channel ch : this) {
            if (channelMatcher.match(ch)) {
                newGroup.add(ch);
            }
        }
        return newGroup;
    }

    @Override
    public boolean isEmpty() {
        return nonServerChannels.isEmpty() && serverChannels.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Channel)) {
            return false;
        }

        Set<Channel> set = o instanceof ServerChannel ? serverChannels : nonServerChannels;
        return set.contains(o);
    }

    @Override
    public Object[] toArray() {
        if (isEmpty()) {
            return EMPTY_ARRAY;
        }

        Object[] o = new Object[size()];
        System.arraycopy(nonServerChannels.toArray(), 0, o, 0, nonServerChannels.size());
        System.arraycopy(serverChannels.toArray(), 0, o, nonServerChannels.size() - 1, serverChannels.size());
        return o;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        if (isEmpty()) {
            return (T[]) EMPTY_ARRAY;
        }

        T[] o = (T[]) Array.newInstance(a.getClass().getComponentType(), size());
        System.arraycopy(nonServerChannels.toArray(), 0, o, 0, nonServerChannels.size());
        System.arraycopy(serverChannels.toArray(), 0, o, nonServerChannels.size() - 1, serverChannels.size());
        return o;
    }

    @Override
    public boolean add(Channel channel) {
        Set<Channel> set = channel instanceof ServerChannel ? serverChannels : nonServerChannels;
        boolean added = set.add(channel);
        if (added) {
            channel.closeFuture().addListener(remover);
        }
        return added;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Channel)) {
            return false;
        }

        Set<Channel> set = o instanceof ServerChannel ? serverChannels : nonServerChannels;
        Channel ch = (Channel) o;
        boolean removed = set.remove(o);
        if (removed) {
            ch.closeFuture().removeListener(remover);
        }
        return removed;
    }

    @Override
    public void clear() {
        nonServerChannels.clear();
        serverChannels.clear();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int compareTo(ChannelGroup o) {
        int v = name().compareTo(o.name());
        if (v != 0) {
            return v;
        }

        return System.identityHashCode(this) - System.identityHashCode(o);
    }

    @Override
    public String toString() {
        return getClass().getName() + "(name: " + name() + ", size: " + size() + ')';
    }
}
