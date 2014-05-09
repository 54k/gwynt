package io.gwynt.core;

import io.gwynt.core.scheduler.EventScheduler;

public interface Endpoint {

    /**
     * Add filter from {@link io.gwynt.core.pipeline.Pipeline} for all incoming {@link IoSession}s
     *
     * @param ioHandler {@link IoHandler} implementation
     * @return current {@link Endpoint}
     */
    Endpoint addHandler(IoHandler ioHandler);

    /**
     * Remove filter from {@link io.gwynt.core.pipeline.Pipeline} for all incoming {@link IoSession}s
     *
     * @param ioHandler {@link IoHandler} implementation
     * @return current {@link Endpoint}
     */
    Endpoint removeHandler(IoHandler ioHandler);

    /**
     * Retrieve all attached {@link IoHandler} for this {@link Endpoint}
     *
     * @return current attached filters
     */
    Iterable<IoHandler> getHandlers();

    /**
     * Get specific {@link IoSessionFactory} implementation
     *
     * @return current {@link IoSessionFactory}
     */
    IoSessionFactory getSessionFactory();

    /**
     * Set specific {@link IoSessionFactory} implementation
     *
     * @param ioSessionFactory {@link IoSessionFactory} implementation
     * @return current {@link Endpoint}
     */
    Endpoint setSessionFactory(IoSessionFactory ioSessionFactory);

    /**
     * Returns instance of {@link io.gwynt.core.scheduler.EventScheduler} in which all {@link IoHandler} events dispatched
     *
     * @return current {@link io.gwynt.core.scheduler.EventScheduler}
     */
    EventScheduler getScheduler();

    /**
     * Set specific {@link io.gwynt.core.scheduler.EventScheduler} implementation, which will dispatch all events to {@link IoHandler}
     *
     * @param eventScheduler {@link io.gwynt.core.scheduler.EventScheduler} implementation
     * @return current {@link Endpoint}
     */
    Endpoint setScheduler(EventScheduler eventScheduler);

    /**
     * Synchronously start listening incoming connections
     *
     * @param port desired port
     */
    Endpoint bind(int port);

    /**
     * Synchronously stop listening incoming connections
     */
    Endpoint unbind();
}
