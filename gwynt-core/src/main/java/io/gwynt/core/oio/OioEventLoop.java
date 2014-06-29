package io.gwynt.core.oio;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelFutureListener;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.MultiThreadEventLoopGroup;
import io.gwynt.core.SingleThreadEventLoop;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;


public class OioEventLoop extends SingleThreadEventLoop {

    private Channel ch;

    public OioEventLoop(MultiThreadEventLoopGroup parent, Executor executor) {
        super(parent, true, executor);
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
        this.ch = null;
    }
}
