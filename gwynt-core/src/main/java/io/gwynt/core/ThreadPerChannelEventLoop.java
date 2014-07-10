package io.gwynt.core;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public final class ThreadPerChannelEventLoop extends SingleThreadEventLoop {

    private Channel ch;
    private ThreadPerChannelEventLoopGroup parent;

    ThreadPerChannelEventLoop(ThreadPerChannelEventLoopGroup parent) {
        super(parent, true, parent.executor);
        this.parent = parent;
    }

    @Override
    protected Queue<Runnable> newTaskQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return register(channel, new DefaultChannelPromise(channel, this));
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise channelPromise) {
        return super.register(channel, channelPromise).addListener(new ChannelFutureListener() {
            @Override
            public void onComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    ch = future.channel();
                } else {
                    unregister();
                }
            }
        });
    }

    @Override
    public ChannelFuture unregister(Channel channel) {
        return unregister(channel, new DefaultChannelPromise(channel, this));
    }

    @Override
    public ChannelFuture unregister(Channel channel, ChannelPromise channelPromise) {
        return super.unregister(channel, channelPromise).addListener(new ChannelFutureListener() {
            @Override
            public void onComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    unregister();
                }
            }
        });
    }

    @Override
    protected void run() {
        for (; ; ) {
            Runnable task = takeTask();
            if (task != null) {
                task.run();
            }

            Channel ch = this.ch;
            if (isShuttingDown()) {
                if (ch != null) {
                    ch.unsafe().close(ch.voidPromise());
                }
                if (confirmShutdown()) {
                    break;
                }
            } else {
                if (ch != null) {
                    if (!ch.isRegistered()) {
                        runAllTasks();
                        unregister();
                    }
                }
            }
        }
    }

    private void unregister() {
        ch = null;
        parent.activeChildren.remove(this);
        parent.idleChildren.add(this);
    }
}
