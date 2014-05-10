package io.gwynt.core;

import io.gwynt.core.pipeline.IoHandlerContext;

/**
 * Every {@link io.gwynt.core.IoSession} has own instance of {@link io.gwynt.core.pipeline.Pipeline} which intercepts events occurred on session. <br/>
 * Each {@link IoHandler} processed in {@link IoHandler}'s thread. <br/>
 * Inbound events processed from first to last filter. Outbound events processed from last to first.
 *
 * @param <I> Type of inbound high-level message. First filter in {@link io.gwynt.core.pipeline.Pipeline} receives array of bytes.
 * @param <O> Type of outbound high-level message. Last filter in {@link io.gwynt.core.pipeline.Pipeline} should return array of bytes.
 */
public interface IoHandler<I, O> {
    /**
     * Called when {@link io.gwynt.core.IoSession} is registered by {@link io.gwynt.core.transport.AbstractDispatcher}
     *
     * @param context {@link io.gwynt.core.pipeline.IoHandlerContext}
     */
    void onRegistered(IoHandlerContext context);

    /**
     * Called when {@link io.gwynt.core.IoSession} is unregistered by {@link io.gwynt.core.transport.AbstractDispatcher}
     *
     * @param context {@link io.gwynt.core.pipeline.IoHandlerContext}
     */
    void onUnregistered(IoHandlerContext context);

    /**
     * Called when {@link io.gwynt.core.IoSession} is fully established
     *
     * @param context {@link io.gwynt.core.pipeline.IoHandlerContext}
     */
    void onOpen(IoHandlerContext context);

    /**
     * Called when inbound message received
     *
     * @param context {@link IoHandlerContext}
     * @param message high level message
     */
    void onMessageReceived(IoHandlerContext context, I message);

    /**
     * Called when outbound message sent
     *
     * @param context {@link IoHandlerContext}
     * @param message high level message
     */
    void onMessageSent(IoHandlerContext context, O message);

    /**
     * Called when {@link io.gwynt.core.IoSession} is requested to close
     *
     * @param context {@link IoHandlerContext}
     */
    void onClosing(IoHandlerContext context);

    /**
     * Called when {@link io.gwynt.core.IoSession} is fully closed
     *
     * @param context {@link IoHandlerContext}
     */
    void onClose(IoHandlerContext context);

    /**
     * Called when error occurs
     *
     * @param context {@link IoHandlerContext}
     */
    void onExceptionCaught(IoHandlerContext context, Throwable e);
}
