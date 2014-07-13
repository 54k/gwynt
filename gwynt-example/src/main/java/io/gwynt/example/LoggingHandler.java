package io.gwynt.example;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.pipeline.HandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by user on 26.05.2014.
 */
class LoggingHandler extends AbstractHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoggingHandler.class);

    @Override
    public void onExceptionCaught(HandlerContext context, Throwable e) {
        logger.info("Caught exception [{}] on channel [{}]", e, context.channel());
        logger.error(e.getMessage(), e);
        context.fireExceptionCaught(e);
    }

    @Override
    public void onClose(HandlerContext context) {
        logger.info("Closed channel [{}]", context.channel());
        context.fireClose();
    }

    @Override
    public void onClosing(HandlerContext context, final ChannelPromise channelPromise) {
        logger.info("Closing channel [{}]", context.channel());
        channelPromise.addListener(new ChannelFutureListener() {
            @Override
            public void onComplete(ChannelFuture channelFuture) {
                logger.info("Closed channel [{}], channelPromise [{}]", channelFuture.channel(), channelFuture);
            }
        });
        context.close(channelPromise);
    }

    @Override
    public void onMessageSent(final HandlerContext context, final Object message, final ChannelPromise channelPromise) {
        logger.info("Sending message [{}] to channel [{}]", message, context.channel());
        channelPromise.addListener(new ChannelFutureListener() {
            @Override
            public void onComplete(ChannelFuture channelFuture) {
                if (channelFuture.isSuccess()) {
                    logger.info("Sent to channel [{}], channelPromise [{}], message [{}]", channelFuture.channel(), channelPromise, message);
                } else {
                    logger.info("Failed sent to channel [{}], channelPromise [{}], message [{}]", channelFuture.channel(), channelPromise, message);
                }
            }
        });
        context.write(message, channelPromise);
    }

    @Override
    public void onMessageReceived(HandlerContext context, Object message) {
        logger.info("Received message [{}] from channel [{}]", message, context.channel());
        context.fireMessageReceived(message);
    }

    @Override
    public void onOpen(HandlerContext context) {
        logger.info("Opened channel [{}]", context.channel());
        context.fireOpen();
    }

    @Override
    public void onUnregistered(HandlerContext context) {
        logger.info("Unregistered channel [{}]", context.channel());
        context.fireUnregistered();
    }

    @Override
    public void onRegistered(HandlerContext context) {
        logger.info("Registered Channel [{}]", context.channel());
        context.fireRegistered();
    }
}
