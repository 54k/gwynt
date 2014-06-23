package io.gwynt.core.pipeline;

import io.gwynt.core.ChannelPromise;
import io.gwynt.core.concurrent.EventExecutor;

import static io.gwynt.core.pipeline.HandlerContextInvokerUtils.invokeOnCloseNow;
import static io.gwynt.core.pipeline.HandlerContextInvokerUtils.invokeOnClosingNow;
import static io.gwynt.core.pipeline.HandlerContextInvokerUtils.invokeOnDisconnectNow;
import static io.gwynt.core.pipeline.HandlerContextInvokerUtils.invokeOnExceptionCaughtNow;
import static io.gwynt.core.pipeline.HandlerContextInvokerUtils.invokeOnMessageReceivedNow;
import static io.gwynt.core.pipeline.HandlerContextInvokerUtils.invokeOnMessageSentNow;
import static io.gwynt.core.pipeline.HandlerContextInvokerUtils.invokeOnOpenNow;
import static io.gwynt.core.pipeline.HandlerContextInvokerUtils.invokeOnReadNow;
import static io.gwynt.core.pipeline.HandlerContextInvokerUtils.invokeOnRegisteredNow;
import static io.gwynt.core.pipeline.HandlerContextInvokerUtils.invokeOnUnregisteredNow;

public class DefaultHandlerContextInvoker implements HandlerContextInvoker {

    private EventExecutor executor;

    public DefaultHandlerContextInvoker(EventExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void invokeOnRegistered(final HandlerContext context) {
        if (executor.inExecutorThread()) {
            invokeOnRegisteredNow(context);
        } else {
            DefaultHandlerContext dctx = (DefaultHandlerContext) context;
            Runnable event = dctx.registeredEvent;
            if (event == null) {
                dctx.registeredEvent = event = new Runnable() {
                    @Override
                    public void run() {
                        invokeOnRegisteredNow(context);
                    }
                };
            }
            executor.execute(event);
        }
    }

    @Override
    public void invokeOnUnregistered(final HandlerContext context) {
        if (executor.inExecutorThread()) {
            invokeOnUnregisteredNow(context);
        } else {
            DefaultHandlerContext dctx = (DefaultHandlerContext) context;
            Runnable event = dctx.unregisteredEvent;
            if (event == null) {
                dctx.unregisteredEvent = event = new Runnable() {
                    @Override
                    public void run() {
                        invokeOnUnregisteredNow(context);
                    }
                };
            }
            executor.execute(event);
        }
    }

    @Override
    public void invokeOnOpen(final HandlerContext context) {
        if (executor.inExecutorThread()) {
            invokeOnOpenNow(context);
        } else {
            DefaultHandlerContext dctx = (DefaultHandlerContext) context;
            Runnable event = dctx.openEvent;
            if (event == null) {
                dctx.openEvent = event = new Runnable() {
                    @Override
                    public void run() {
                        invokeOnOpenNow(context);
                    }
                };
            }
            executor.execute(event);
        }
    }

    @Override
    public void invokeOnRead(final HandlerContext context, final ChannelPromise channelPromise) {
        if (executor.inExecutorThread()) {
            invokeOnReadNow(context, channelPromise);
        } else {
            DefaultHandlerContext dctx = (DefaultHandlerContext) context;
            Runnable event = dctx.readEvent;
            if (event == null) {
                dctx.readEvent = event = new Runnable() {
                    @Override
                    public void run() {
                        invokeOnReadNow(context, channelPromise);
                    }
                };
            }
            executor.execute(event);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invokeOnMessageReceived(final HandlerContext context, final Object message) {
        if (executor.inExecutorThread()) {
            invokeOnMessageReceivedNow(context, message);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    invokeOnMessageReceivedNow(context, message);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invokeOnMessageSent(final HandlerContext context, final Object message, final ChannelPromise channelPromise) {
        if (executor.inExecutorThread()) {
            invokeOnMessageSentNow(context, message, channelPromise);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    invokeOnMessageSentNow(context, message, channelPromise);
                }
            });
        }
    }

    @Override
    public void invokeOnClosing(final HandlerContext context, final ChannelPromise channelPromise) {
        if (executor.inExecutorThread()) {
            invokeOnClosingNow(context, channelPromise);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    invokeOnClosingNow(context, channelPromise);
                }
            });
        }
    }

    @Override
    public void invokeOnClosed(final HandlerContext context) {
        if (executor.inExecutorThread()) {
            invokeOnCloseNow(context);
        } else {
            DefaultHandlerContext dctx = (DefaultHandlerContext) context;
            Runnable event = dctx.closeEvent;
            if (event == null) {
                dctx.closeEvent = event = new Runnable() {
                    @Override
                    public void run() {
                        invokeOnCloseNow(context);
                    }
                };
            }
            executor.execute(event);
        }
    }

    @Override
    public void invokeOnDisconnect(final HandlerContext context, final ChannelPromise channelPromise) {
        if (executor.inExecutorThread()) {
            invokeOnDisconnectNow(context, channelPromise);
        } else {
            DefaultHandlerContext dctx = (DefaultHandlerContext) context;
            Runnable event = dctx.disconnectEvent;
            if (event == null) {
                dctx.disconnectEvent = event = new Runnable() {
                    @Override
                    public void run() {
                        invokeOnDisconnectNow(context, channelPromise);
                    }
                };
            }
            executor.execute(event);
        }
    }

    @Override
    public void invokeOnExceptionCaught(final HandlerContext context, final Throwable e) {
        if (executor.inExecutorThread()) {
            invokeOnExceptionCaughtNow(context, e);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    invokeOnExceptionCaughtNow(context, e);
                }
            });
        }
    }
}
