package io.gwynt.core.pipeline;

import io.gwynt.core.ChannelPromise;
import io.gwynt.core.EventExecutor;

public class DefaultHandlerContextInvoker implements HandlerContextInvoker {

    private EventExecutor executor;

    public DefaultHandlerContextInvoker(EventExecutor executor) {
        this.executor = executor;
    }

    private static void invokeOnHandlerAddedNow(HandlerContext context) {
        try {
            context.handler().onHandlerAdded(context);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnHandlerRemovedNow(HandlerContext context) {
        try {
            context.handler().onHandlerRemoved(context);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnRegisteredNow(HandlerContext context) {
        try {
            context.handler().onRegistered(context);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnUnregisteredNow(HandlerContext context) {
        try {
            context.handler().onUnregistered(context);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnOpenNow(HandlerContext context) {
        try {
            context.handler().onOpen(context);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnReadNow(HandlerContext context, ChannelPromise channelPromise) {
        try {
            context.handler().onRead(context, channelPromise);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void invokeOnMessageReceivedNow(HandlerContext context, Object message) {
        try {
            context.handler().onMessageReceived(context, message);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void invokeOnMessageSentNow(HandlerContext context, Object message, ChannelPromise channelPromise) {
        try {
            context.handler().onMessageSent(context, message, channelPromise);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnClosingNow(HandlerContext context, ChannelPromise channelPromise) {
        try {
            context.handler().onClosing(context, channelPromise);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnCloseNow(HandlerContext context) {
        try {
            context.handler().onClose(context);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnExceptionCaughtNow(HandlerContext context, Throwable e) {
        context.handler().onExceptionCaught(context, e);
    }

    @Override
    public void invokeOnHandlerAdded(final HandlerContext context) {
        if (executor.inExecutorThread()) {
            invokeOnHandlerAddedNow(context);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    invokeOnHandlerAddedNow(context);
                }
            });
        }
    }

    @Override
    public void invokeOnHandlerRemoved(final HandlerContext context) {
        if (executor.inExecutorThread()) {
            invokeOnHandlerRemovedNow(context);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    invokeOnHandlerRemovedNow(context);
                }
            });
        }
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
