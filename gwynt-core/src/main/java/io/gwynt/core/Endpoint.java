package io.gwynt.core;

import io.gwynt.core.scheduler.EventScheduler;
import io.gwynt.core.transport.Dispatcher;

public interface Endpoint {

    /**
     * Add handler from {@link io.gwynt.core.pipeline.Pipeline} for all incoming {@link Channel}s
     *
     * @param handler {@link Handler} implementation
     * @return current {@link Endpoint}
     */
    Endpoint addHandler(Handler handler);

    /**
     * Remove handler from {@link io.gwynt.core.pipeline.Pipeline} for all incoming {@link Channel}s
     *
     * @param handler {@link Handler} implementation
     * @return current {@link Endpoint}
     */
    Endpoint removeHandler(Handler handler);

    /**
     * Retrieve all attached {@link Handler} for this {@link Endpoint}
     *
     * @return current attached filters
     */
    Iterable<Handler> getHandlers();

    /**
     * Get specific {@link ChannelFactory} implementation
     *
     * @return current {@link ChannelFactory}
     */
    ChannelFactory getChannelFactory();

    /**
     * Set specific {@link ChannelFactory} implementation
     *
     * @param channelFactory {@link ChannelFactory} implementation
     * @return current {@link Endpoint}
     */
    Endpoint setChannelFactory(ChannelFactory channelFactory);

    /**
     * Get specific {@link io.gwynt.core.Channel} class
     *
     * @return current {@link io.gwynt.core.Channel class}
     */
    Class<? extends Channel> getChannelClass();

    /**
     * Set specific {@link io.gwynt.core.Channel} class
     *
     * @return current {@link Endpoint}
     */
    Endpoint setChannelClass(Class<? extends Channel> channel);

    /**
     * Returns instance of {@link io.gwynt.core.scheduler.EventScheduler} in which all {@link Handler} events dispatched
     *
     * @return current {@link io.gwynt.core.scheduler.EventScheduler}
     */
    EventScheduler getScheduler();

    /**
     * Set specific {@link io.gwynt.core.scheduler.EventScheduler} implementation, which will dispatch all events to {@link Handler}
     *
     * @param eventScheduler {@link io.gwynt.core.scheduler.EventScheduler} implementation
     * @return current {@link Endpoint}
     */
    Endpoint setScheduler(EventScheduler eventScheduler);

    /**
     * Returns instance of {@link Dispatcher} in which all {@link Channel} io events dispatched
     *
     * @return current {@link Dispatcher}
     */
    Dispatcher getDispatcher();

    /**
     * Set specific {@link Dispatcher} implementation, which will dispatch all io events to {@link Channel}
     *
     * @param dispatcher {@link Dispatcher} implementation
     * @return current {@link Endpoint}
     */
    Endpoint setDispatcher(Dispatcher dispatcher);

    /**
     * Start listening for incoming connections
     *
     * @param port desired port
     */
    ChannelFuture bind(int port);

    /**
     * Connect {@link io.gwynt.core.Channel} to specified host and port
     *
     * @param port desired port
     */
    ChannelFuture connect(String host, int port);

    /**
     * Stop listening for incoming connections
     */
    Endpoint shutdown();
}
