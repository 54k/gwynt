package io.gwynt.core.pipeline;

import io.gwynt.core.Channel;
import io.gwynt.core.Handler;

public final class DefaultHandlerContext extends AbstractHandlerContext {

    private Handler handler;

    public DefaultHandlerContext(Channel channel, Handler handler) {
        this(null, channel, handler);
    }

    public DefaultHandlerContext(HandlerContextInvoker invoker, Channel channel, Handler handler) {
        super(invoker, channel);
        this.handler = handler;
    }

    @Override
    public Handler handler() {
        return handler;
    }
}
