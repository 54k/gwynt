package io.gwynt.core;

import io.gwynt.core.exception.ChannelException;
import io.gwynt.core.pipeline.DefaultHandlerContextInvoker;
import io.gwynt.core.pipeline.HandlerContextInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractEventScheduler implements EventScheduler {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractEventScheduler.class);
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final HandlerContextInvoker invoker = new DefaultHandlerContextInvoker(this);
    private Thread thread;
    private volatile boolean running;

    @Override
    public HandlerContextInvoker asInvoker() {
        return invoker;
    }

    @Override
    public void schedule(Runnable task) {
        addTask(task);
    }

    protected boolean hasTasks() {
        return !tasks.isEmpty();
    }

    protected void addTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }
        tasks.add(task);
    }

    protected void runTasks(long timeout) {
        long elapsedTime = 0;
        Runnable task;
        while ((task = tasks.poll()) != null) {
            long startTime = System.currentTimeMillis();
            try {
                task.run();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
            elapsedTime += System.currentTimeMillis() - startTime;
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

    @Override
    public boolean inSchedulerThread() {
        return thread == Thread.currentThread();
    }

    public void runThread() {
        if (running) {
            return;
        }
        running = true;
        Thread workerThread = new Thread(this);
        workerThread.start();
        thread = workerThread;
    }

    public void shutdownThread() {
        if (!running) {
            return;
        }
        running = false;
        thread = null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
