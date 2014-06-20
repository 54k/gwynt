package io.gwynt.core.pipeline;

import io.gwynt.core.ChannelPromise;

public final class HandlerContextInvokerUtils {

    private HandlerContextInvokerUtils() {
    }

    public static void invokeOnRegisteredNow(HandlerContext context) {
        try {
            context.handler().onRegistered(context);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    public static void invokeOnUnregisteredNow(HandlerContext context) {
        try {
            context.handler().onUnregistered(context);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    public static void invokeOnOpenNow(HandlerContext context) {
        try {
            context.handler().onOpen(context);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    public static void invokeOnReadNow(HandlerContext context, ChannelPromise channelPromise) {
        try {
            context.handler().onRead(context, channelPromise);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void invokeOnMessageReceivedNow(HandlerContext context, Object message) {
        try {
            context.handler().onMessageReceived(context, message);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void invokeOnMessageSentNow(HandlerContext context, Object message, ChannelPromise channelPromise) {
        try {
            context.handler().onMessageSent(context, message, channelPromise);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    public static void invokeOnClosingNow(HandlerContext context, ChannelPromise channelPromise) {
        try {
            context.handler().onClosing(context, channelPromise);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    public static void invokeOnCloseNow(HandlerContext context) {
        try {
            context.handler().onClose(context);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    public static void invokeOnDisconnectNow(HandlerContext context, ChannelPromise channelPromise) {
        try {
            context.handler().onDisconnect(context, channelPromise);
        } catch (Throwable e) {
            context.handler().onExceptionCaught(context, e);
        }
    }

    public static void invokeOnExceptionCaughtNow(HandlerContext context, Throwable e) {
        context.handler().onExceptionCaught(context, e);
    }
}
