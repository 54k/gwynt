package io.gwynt.core.scheduler;

import io.gwynt.core.Channel;
import io.gwynt.core.ChannelFuture;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.exception.ChannelException;
import io.gwynt.core.pipeline.DefaultHandlerContextInvoker;
import io.gwynt.core.pipeline.HandlerContextInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractEventScheduler extends AbstractExecutorService implements EventScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEventScheduler.class);

    private Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private HandlerContextInvoker invoker = new DefaultHandlerContextInvoker(this);

    protected void addTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }
        tasks.add(task);
    }

    protected void runTasks(long timeout) {
        long elapsedTime = 0;
        while (tasks.peek() != null) {
            long startTime = System.nanoTime();
            try {
                tasks.poll().run();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
            elapsedTime += System.nanoTime() - startTime;
            if (elapsedTime >= timeout) {
                break;
            }
        }
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
        addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    channel.unsafe().register(AbstractEventScheduler.this);
                    channelPromise.complete();
                } catch (ChannelException e) {
                    channelPromise.complete(e);
                }
            }
        });
        return channelPromise;
    }

    @Override
    public ChannelFuture unregister(final Channel channel, final ChannelPromise channelPromise) {
        if (channel == null || channelPromise == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    channel.unsafe().unregister();
                    channelPromise.complete();
                } catch (ChannelException e) {
                    channelPromise.complete(e);
                }
            }
        });
        return channelPromise;
    }
}
