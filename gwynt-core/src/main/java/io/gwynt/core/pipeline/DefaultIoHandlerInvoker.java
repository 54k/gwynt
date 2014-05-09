package io.gwynt.core.pipeline;

import io.gwynt.core.scheduler.EventScheduler;

public class DefaultIoHandlerInvoker implements IoHandlerInvoker {

    private EventScheduler scheduler;

    public DefaultIoHandlerInvoker(EventScheduler scheduler) {
        this.scheduler = scheduler;
    }

    private static void invokeOnOpenNow(IoHandlerContext context) {
        try {
            context.getIoHandler().onOpen(context);
        } catch (Throwable e) {
            context.getIoHandler().onExceptionCaught(context, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void invokeOnMessageReceivedNow(IoHandlerContext context, Object message) {
        try {
            context.getIoHandler().onMessageReceived(context, message);
        } catch (Throwable e) {
            context.getIoHandler().onExceptionCaught(context, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void invokeOnMessageSentNow(IoHandlerContext context, Object message) {
        try {
            context.getIoHandler().onMessageSent(context, message);
        } catch (Throwable e) {
            context.getIoHandler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnClosingNow(IoHandlerContext context) {
        try {
            context.getIoHandler().onClosing(context);
        } catch (Throwable e) {
            context.getIoHandler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnCloseNow(IoHandlerContext context) {
        try {
            context.getIoHandler().onClose(context);
        } catch (Throwable e) {
            context.getIoHandler().onExceptionCaught(context, e);
        }
    }

    private static void invokeOnExceptionCaughtNow(IoHandlerContext context, Throwable e) {
        context.getIoHandler().onExceptionCaught(context, e);
    }

    @Override
    public void invokeOnOpen(final IoHandlerContext context) {
        if (scheduler.inSchedulerThread()) {
            invokeOnOpenNow(context);
        } else {
            DefaultIoHandlerContext dctx = (DefaultIoHandlerContext) context;
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
    public void invokeOnMessageReceived(final IoHandlerContext context, final Object message) {
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
    public void invokeOnMessageSent(final IoHandlerContext context, final Object message) {
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
    public void invokeOnClosing(final IoHandlerContext context) {
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
    public void invokeOnClosed(final IoHandlerContext context) {
        if (scheduler.inSchedulerThread()) {
            invokeOnCloseNow(context);
        } else {
            DefaultIoHandlerContext dctx = (DefaultIoHandlerContext) context;
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
    public void invokeOnExceptionCaught(final IoHandlerContext context, final Throwable e) {
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
