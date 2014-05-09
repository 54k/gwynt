package io.gwynt.core;

import io.gwynt.core.scheduler.EventScheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractEndpoint implements Endpoint {

    protected List<IoHandler> ioHandlers = new ArrayList<>();
    protected IoSessionFactory ioSessionFactory;
    protected EventScheduler eventScheduler;

    @Override
    public Endpoint addHandler(IoHandler ioHandler) {
        if (ioHandler == null) {
            throw new IllegalArgumentException("filter");
        }

        ioHandlers.add(ioHandler);
        return this;
    }

    @Override
    public Endpoint removeHandler(IoHandler ioHandler) {
        if (ioHandler == null) {
            throw new IllegalArgumentException("filter");
        }

        ioHandlers.remove(ioHandler);
        return this;
    }

    @Override
    public Iterable<IoHandler> getHandlers() {
        return Collections.unmodifiableList(ioHandlers);
    }

    @Override
    public IoSessionFactory getSessionFactory() {
        return ioSessionFactory;
    }

    @Override
    public Endpoint setSessionFactory(IoSessionFactory ioSessionFactory) {
        if (ioSessionFactory == null) {
            throw new IllegalArgumentException("connectionFactory");
        }

        this.ioSessionFactory = ioSessionFactory;
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
