package io.gwynt.core;

import io.gwynt.core.pipeline.HandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ChannelInitializer extends AbstractHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChannelInitializer.class);

    protected abstract void initialize(Channel channel);

    @Override
    public void onRegistered(HandlerContext context) {
        try {
            initialize(context.channel());
            context.channel().pipeline().remove(context.name());
            super.onRegistered(context);
        } catch (Throwable e) {
            logger.error("Exception caught while initializing session. {} will be closed.", context.channel());
            context.close();
        }
    }
}
