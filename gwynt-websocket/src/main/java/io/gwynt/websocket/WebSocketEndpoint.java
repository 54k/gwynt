package io.gwynt.websocket;

import io.gwynt.core.Endpoint;
import io.gwynt.core.IoHandler;
import io.gwynt.core.IoSessionFactory;
import io.gwynt.core.NioEndpoint;
import io.gwynt.core.scheduler.EventScheduler;

public class WebSocketEndpoint implements Endpoint {

    private NioEndpoint endpoint;

    public WebSocketEndpoint() {
        this.endpoint = new NioEndpoint();
        this.endpoint.addHandler(null);
    }

    @Override
    public Endpoint bind(int port) {
        return endpoint.bind(port);
    }

    @Override
    public Endpoint unbind() {
        return endpoint.unbind();
    }

    @Override
    public Endpoint addHandler(IoHandler ioHandler) {
        return endpoint.addHandler(ioHandler);
    }

    @Override
    public Endpoint removeHandler(IoHandler ioHandler) {
        return endpoint.removeHandler(ioHandler);
    }

    @Override
    public Iterable<IoHandler> getHandlers() {
        return endpoint.getHandlers();
    }

    @Override
    public IoSessionFactory getSessionFactory() {
        return endpoint.getSessionFactory();
    }

    @Override
    public Endpoint setSessionFactory(IoSessionFactory ioSessionFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventScheduler getScheduler() {
        return endpoint.getScheduler();
    }

    @Override
    public Endpoint setScheduler(EventScheduler eventScheduler) {
        return endpoint.setScheduler(eventScheduler);
    }
}
