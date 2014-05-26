package io.gwynt.core;

import io.gwynt.core.exception.ChannelException;
import io.gwynt.core.pipeline.DefaultHandlerContextInvoker;
import io.gwynt.core.pipeline.HandlerContextInvoker;

public abstract class AbstractEventLoop extends AbstractEventExecutor implements EventLoop, EventLoopGroup {

    private final HandlerContextInvoker invoker = new DefaultHandlerContextInvoker(this);

    @Override
    public HandlerContextInvoker asInvoker() {
        return invoker;
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return register(channel, channel.newChannelPromise());
    }

    @Override
    public ChannelFuture unregister(Channel channel) {
        return unregister(channel, channel.newChannelPromise());
    }

    @Override
    public ChannelFuture register(final Channel channel, final ChannelPromise channelPromise) {
        if (channel == null || channelPromise == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        if (inExecutorThread()) {
            register0(channel, channelPromise);
        } else {
            execute(new Runnable() {
                @Override
                public void run() {
                    register0(channel, channelPromise);
                }
            });
        }
        return channelPromise;
    }

    private void register0(Channel channel, ChannelPromise channelPromise) {
        try {
            channel.unsafe().register(AbstractEventLoop.this);
            channelPromise.setSuccess();
        } catch (ChannelException e) {
            channelPromise.setFailure(e);
        }
    }

    @Override
    public ChannelFuture unregister(final Channel channel, final ChannelPromise channelPromise) {
        if (channel == null || channelPromise == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        if (inExecutorThread()) {
            unregister0(channel, channelPromise);
        } else {
            execute(new Runnable() {
                @Override
                public void run() {
                    unregister0(channel, channelPromise);
                }
            });
        }
        return channelPromise;
    }

    private void unregister0(Channel channel, ChannelPromise channelPromise) {
        try {
            channel.unsafe().unregister();
            channelPromise.setSuccess();
        } catch (ChannelException e) {
            channelPromise.setFailure(e);
        }
    }
}
