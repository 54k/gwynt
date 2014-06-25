package io.gwynt.core.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public final class GlobalEventExecutor extends AbstractScheduledEventExecutor {

    public static final EventExecutor INSTANCE = new GlobalEventExecutor();

    private static final Logger logger = LoggerFactory.getLogger(GlobalEventExecutor.class);

    private static final AtomicIntegerFieldUpdater<GlobalEventExecutor> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(GlobalEventExecutor.class, "state");

    private static final int ST_NOT_STARTED = 1;
    @SuppressWarnings("FieldCanBeLocal")
    private volatile int state = ST_NOT_STARTED;
    private static final int ST_STARTED = 2;
    private static final Runnable WAKEUP_TASK = new Runnable() {
        @Override
        public void run() {
            // Do nothing.
        }
    };
    private final boolean wakeUpForTask;
    private final Executor executor;
    private Thread thread;
    private Queue<Runnable> taskQueue = newTaskQueue();

    private GlobalEventExecutor() {
        executor = new ThreadPerTaskExecutor(new DefaultThreadFactory("global-event-executor-"));
        wakeUpForTask = true;
    }

    protected Queue<Runnable> newTaskQueue() {
        return new LinkedBlockingQueue<>();
    }

    protected Runnable takeTask() {
        if (!(this.taskQueue instanceof BlockingQueue)) {
            throw new IllegalArgumentException("taskQueue is not instanceof BlockingQueue");
        }

        BlockingQueue<Runnable> taskQueue = (BlockingQueue<Runnable>) this.taskQueue;
        for (; ; ) {
            ScheduledFutureTask<?> delayedTask = peekDelayedTask();

            if (delayedTask == null) {
                return taskQueue.poll();
            } else {
                long delayNanos = delayedTask.getDelayNanos();
                Runnable task;
                if (delayNanos > 0) {
                    try {
                        task = taskQueue.poll(delayNanos, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        // Waken up.
                        return null;
                    }
                } else {
                    task = taskQueue.poll();
                }

                if (task == null) {
                    fetchDelayedTasks();
                    task = taskQueue.poll();
                }

                if (task != null) {
                    return task;
                }
            }
        }
    }

    protected void fetchDelayedTasks() {
        for (; ; ) {
            ScheduledFutureTask<?> delayedTask = fetchDelayedTask();
            if (delayedTask == null) {
                break;
            }
            taskQueue.add(delayedTask);
        }
    }

    protected boolean hasTasks() {
        return !taskQueue.isEmpty();
    }

    protected void addTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }
        taskQueue.add(task);
    }

    @Deprecated
    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Void> shutdownGracefully() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Void> terminationFuture() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShuttingDown() {
        return false;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Deprecated
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(Runnable command) {
        addTask(command);
        if (STATE_UPDATER.get(this) == ST_NOT_STARTED) {
            startThread();
        }
    }

    @Override
    public boolean inExecutorThread(Thread thread) {
        return this.thread == thread;
    }

    private void startThread() {
        if (STATE_UPDATER.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
            schedulePurgeTask();
            doStartThread();
        }
    }

    private void doStartThread() {
        assert thread == null;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                GlobalEventExecutor.this.thread = Thread.currentThread();
                try {
                    GlobalEventExecutor.this.run();
                } catch (Throwable e) {
                    logger.warn("Unexpected exception from an event executor: ", e);
                }
                cancelDelayedTasks();
                STATE_UPDATER.set(GlobalEventExecutor.this, ST_NOT_STARTED);
            }
        });
    }

    protected void run() {
        for (; ; ) {
            Runnable task = takeTask();
            if (task != null) {
                try {
                    task.run();
                } catch (Throwable e) {
                    logger.warn("Unexpected exception from an event executor: ", e);
                }
            }

            if (!hasTasks() && pendingTasks() == 1) {
                break;
            }
        }
    }
}
