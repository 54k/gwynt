package io.gwynt.core;

import io.gwynt.core.scheduler.EventScheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractEndpoint implements Endpoint {

    protected List<Handler> handlers = new ArrayList<>();
    protected ChannelFactory channelFactory;
    protected EventScheduler eventScheduler;

    @Override
    public Endpoint addHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("filter");
        }

        handlers.add(handler);
        return this;
    }

    @Override
    public Endpoint removeHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("filter");
        }

        handlers.remove(handler);
        return this;
    }

    @Override
    public Iterable<Handler> getHandlers() {
        return Collections.unmodifiableList(handlers);
    }

    @Override
    public ChannelFactory getChannelFactory() {
        return channelFactory;
    }

    @Override
    public Endpoint setChannelFactory(ChannelFactory channelFactory) {
        if (channelFactory == null) {
            throw new IllegalArgumentException("connectionFactory");
        }

        this.channelFactory = channelFactory;
        return this;
    }

    @Override
    public EventScheduler getScheduler() {
        return eventScheduler;
    }

    @Override
    public Endpoint setScheduler(EventScheduler eventScheduler) {
        if (eventScheduler == null) {
            throw new IllegalArgumentException("scheduler");
        }

        this.eventScheduler = eventScheduler;
        return this;
    }

}
