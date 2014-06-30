package io.gwynt.core;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPerChannelEventLoop extends SingleThreadEventLoop {

    private Channel ch;
    private ThreadPerChannelEventLoopGroup parent;

    public ThreadPerChannelEventLoop(ThreadPerChannelEventLoopGroup parent) {
        super(parent, true, parent.executor);
        this.parent = parent;
    }

    @Override
    protected Queue<Runnable> newTaskQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise channelPromise) {
        return super.register(channel, channelPromise).addListener(new ChannelFutureListener() {
            @Override
            public void onComplete(ChannelFuture future) {
                if (future.isFailed()) {
                    unregister();
                } else {
                    ch = future.channel();
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
                } else {
                    if (confirmShutdown()) {
                        break;
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
