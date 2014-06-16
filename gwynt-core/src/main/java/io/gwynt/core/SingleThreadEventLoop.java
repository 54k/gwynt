package io.gwynt.core;

import io.gwynt.core.concurrent.EventExecutorGroup;
import io.gwynt.core.concurrent.SingleThreadEventExecutor;
import io.gwynt.core.pipeline.DefaultHandlerContextInvoker;
import io.gwynt.core.pipeline.HandlerContextInvoker;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop {

    private final HandlerContextInvoker invoker = new DefaultHandlerContextInvoker(this);

    protected SingleThreadEventLoop(EventExecutorGroup parent, boolean wakeUpForTask) {
        super(parent, wakeUpForTask);
    }

    protected SingleThreadEventLoop(EventExecutorGroup parent, boolean wakeUpForTask, ThreadFactory threadFactory) {
        super(parent, wakeUpForTask, threadFactory);
    }

    protected SingleThreadEventLoop(EventExecutorGroup parent, boolean wakeUpForTask, Executor executor) {
        super(parent, wakeUpForTask, executor);
    }

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
            channel.unsafe().register(SingleThreadEventLoop.this);
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

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    public EventLoopGroup parent() {
        return (EventLoopGroup) super.parent();
    }

    @Override
    protected Queue<Runnable> newTaskQueue() {
        return new ConcurrentLinkedQueue<>();
    }
}
