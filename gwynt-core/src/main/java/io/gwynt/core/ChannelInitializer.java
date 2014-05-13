package io.gwynt.core;

import io.gwynt.core.pipeline.HandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ChannelInitializer extends AbstractHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChannelInitializer.class);

    protected abstract void initialize(Channel session);

    @Override
    public void onRegistered(HandlerContext context) {
        try {
            initialize(context.getChannel());
            context.getChannel().pipeline().remove(context.getName());
            super.onRegistered(context);
        } catch (Throwable e) {
            logger.error("Error occurred while initializing session. {} will be closed.", context.getChannel());
            context.fireClosing();
        }
    }
}
