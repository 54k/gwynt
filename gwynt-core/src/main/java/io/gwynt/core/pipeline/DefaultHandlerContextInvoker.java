package io.gwynt.core.pipeline;

import io.gwynt.core.scheduler.EventScheduler;

public class DefaultHandlerContextInvoker implements HandlerContextInvoker {

    private EventScheduler scheduler;

    public DefaultHandlerContextInvoker(EventScheduler scheduler) {
        this.scheduler = scheduler;
    }

    private static void invokeOnHandlerAddedNow(HandlerContext context) {
        try {
            context.getHandler().onHandlerAdded(context);
        } catch (Throwable e) {
            context.getHandler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnHandlerRemovedNow(HandlerContext context) {
        try {
            context.getHandler().onHandlerRemoved(context);
        } catch (Throwable e) {
            context.getHandler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnRegisteredNow(HandlerContext context) {
        try {
            context.getHandler().onRegistered(context);
        } catch (Throwable e) {
            context.getHandler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnUnregisteredNow(HandlerContext context) {
        try {
            context.getHandler().onUnregistered(context);
        } catch (Throwable e) {
            context.getHandler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnOpenNow(HandlerContext context) {
        try {
            context.getHandler().onOpen(context);
        } catch (Throwable e) {
            context.getHandler().onExceptionCaught(context, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void invokeOnMessageReceivedNow(HandlerContext context, Object message) {
        try {
            context.getHandler().onMessageReceived(context, message);
        } catch (Throwable e) {
            context.getHandler().onExceptionCaught(context, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void invokeOnMessageSentNow(HandlerContext context, Object message) {
        try {
            context.getHandler().onMessageSent(context, message);
        } catch (Throwable e) {
            context.getHandler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnClosingNow(HandlerContext context) {
        try {
            context.getHandler().onClosing(context);
        } catch (Throwable e) {
            context.getHandler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnCloseNow(HandlerContext context) {
        try {
            context.getHandler().onClose(context);
        } catch (Throwable e) {
            context.getHandler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnExceptionCaughtNow(HandlerContext context, Throwable e) {
        context.getHandler().onExceptionCaught(context, e);
    }

    @Override
    public void invokeOnHandlerAdded(final HandlerContext context) {
        if (scheduler.inSchedulerThread()) {
            invokeOnHandlerAddedNow(context);
        } else {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    invokeOnHandlerAddedNow(context);
                }
            });
        }
    }

    @Override
    public void invokeOnHandlerRemoved(final HandlerContext context) {
        if (scheduler.inSchedulerThread()) {
            invokeOnHandlerRemovedNow(context);
        } else {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    invokeOnHandlerRemovedNow(context);
                }
            });
        }
    }

    @Override
    public void invokeOnRegistered(final HandlerContext context) {
        if (scheduler.inSchedulerThread()) {
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
            scheduler.schedule(event);
        }
    }

    @Override
    public void invokeOnUnregistered(final HandlerContext context) {
        if (scheduler.inSchedulerThread()) {
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
            scheduler.schedule(event);
        }
    }

    @Override
    public void invokeOnOpen(final HandlerContext context) {
        if (scheduler.inSchedulerThread()) {
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
            scheduler.schedule(event);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invokeOnMessageReceived(final HandlerContext context, final Object message) {
        if (scheduler.inSchedulerThread()) {
            invokeOnMessageReceivedNow(context, message);
        } else {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    invokeOnMessageReceivedNow(context, message);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invokeOnMessageSent(final HandlerContext context, final Object message) {
        if (scheduler.inSchedulerThread()) {
            invokeOnMessageSentNow(context, message);
        } else {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    invokeOnMessageSentNow(context, message);
                }
            });
        }
    }

    @Override
    public void invokeOnClosing(final HandlerContext context) {
        if (scheduler.inSchedulerThread()) {
            invokeOnClosingNow(context);
        } else {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    invokeOnClosingNow(context);
                }
            });
        }
    }

    @Override
    public void invokeOnClosed(final HandlerContext context) {
        if (scheduler.inSchedulerThread()) {
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
            scheduler.schedule(event);
        }
    }

    @Override
    public void invokeOnExceptionCaught(final HandlerContext context, final Throwable e) {
        if (scheduler.inSchedulerThread()) {
            invokeOnExceptionCaughtNow(context, e);
        } else {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    invokeOnExceptionCaughtNow(context, e);
                }
            });
        }
    }
}