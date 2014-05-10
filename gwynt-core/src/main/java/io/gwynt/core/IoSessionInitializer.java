package io.gwynt.core;

import io.gwynt.core.pipeline.IoHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IoSessionInitializer extends AbstractIoHandler {

    private static final Logger logger = LoggerFactory.getLogger(IoSessionInitializer.class);

    protected abstract void initialize(IoSession session);

    @Override
    public void onRegistered(IoHandlerContext context) {
        try {
            initialize(context.getIoSession());
            context.getIoSession().getPipeline().remove(context.getName());
            super.onRegistered(context);
        } catch (Throwable e) {
            logger.error("Error occurred while initializing session. {} will be closed.", context.getIoSession());
            context.fireClosing();
        }
    }
}
