package io.gwynt.core.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public abstract class SingleThreadEventExecutor extends AbstractScheduledEventExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventExecutor.class);

    private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");

    private static final int ST_NOT_STARTED = 1;
    private static final int ST_STARTED = 2;
    private static final int ST_SHUTTING_DOWN = 3;
    private static final int ST_SHUTDOWN = 4;
    private static final int ST_TERMINATED = 5;
    private static final Runnable WAKEUP_TASK = new Runnable() {
        @Override
        public void run() {
            // Do nothing.
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private volatile int state = ST_NOT_STARTED;

    private final Promise<Void> terminationFuture = new DefaultPromise<>();
    private Thread thread;
    private Queue<Runnable> taskQueue = newTaskQueue();
    private final boolean wakeUpForTask;
    private final Executor executor;
    private int executedTasks = 0;
    private boolean shutdownConfirmed;

    protected SingleThreadEventExecutor(EventExecutorGroup parent, boolean wakeUpForTask) {
        this(parent, wakeUpForTask, new ThreadPerTaskExecutor());
    }

    protected SingleThreadEventExecutor(EventExecutorGroup parent, boolean wakeUpForTask, ThreadFactory threadFactory) {
        this(parent, wakeUpForTask, new ThreadPerTaskExecutor(threadFactory));
    }

    protected SingleThreadEventExecutor(EventExecutorGroup parent, boolean wakeUpForTask, Executor executor) {
        super(parent);
        this.executor = executor;
        this.wakeUpForTask = wakeUpForTask;
    }

    protected static void reject() {
        throw new RejectedExecutionException("event executor terminated");
    }

    protected abstract Queue<Runnable> newTaskQueue();

    protected boolean runAllTasks() {
        assert inExecutorThread();

        fetchDelayedTasks();
        Runnable task = pollTask();
        if (task == null) {
            return false;
        }

        for (; ; ) {
            try {
                task.run();
            } catch (Throwable e) {
                logger.warn("task raised an exception: ", e);
            }

            task = pollTask();

            if (task == null) {
                return true;
            }
        }
    }

    protected boolean runAllTasks(long timeoutNanos) {
        assert inExecutorThread();

        if (timeoutNanos == 0) {
            return runAllTasks();
        }

        fetchDelayedTasks();
        Runnable task = pollTask();
        if (task == null) {
            return false;
        }

        long deadlineNanos = ScheduledFutureTask.triggerTime(timeoutNanos);
        for (; ; ) {
            try {
                task.run();
            } catch (Throwable e) {
                logger.warn("task raised an exception: ", e);
            }

            executedTasks++;
            // check every 32 tasks
            if ((executedTasks & 0x20) != 0) {
                executedTasks = 0;
                if (deadlineNanos >= System.nanoTime()) {
                    return true;
                }
            }

            task = pollTask();

            if (task == null) {
                return true;
            }
        }
    }

    protected long closestDeadlineNanos(long timeNanos) {
        ScheduledFutureTask<?> delayedTask = peekDelayedTask();
        if (delayedTask == null) {
            return -1;
        }

        return delayedTask.getDelayNanos(timeNanos);
    }

    protected Runnable takeTask() {
        if (!(this.taskQueue instanceof BlockingQueue)) {
            throw new IllegalArgumentException("taskQueue is not instanceof BlockingQueue");
        }

        BlockingQueue<Runnable> taskQueue = (BlockingQueue<Runnable>) this.taskQueue;
        for (; ; ) {
            ScheduledFutureTask<?> delayedTask = peekDelayedTask();

            if (delayedTask == null) {
                Runnable task = null;
                try {
                    task = taskQueue.take();
                } catch (InterruptedException ignore) {
                }
                return task;
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

    protected Runnable pollTask() {
        for (; ; ) {
            Runnable task = taskQueue.poll();
            if (task == WAKEUP_TASK) {
                continue;
            }
            return task;
        }
    }

    protected boolean hasTasks() {
        return !taskQueue.isEmpty();
    }

    protected void addTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }

        if (isShutdown()) {
            reject();
        }

        taskQueue.add(task);
    }

    protected void removeTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }

        taskQueue.remove(task);
    }

    @Deprecated
    @Override
    public void shutdown() {
        if (isShutdown()) {
            return;
        }
        if (wakeUpForTask) {
            wakeup(inExecutorThread());
        }

        STATE_UPDATER.set(this, ST_SHUTTING_DOWN);
        thread = null;
    }

    @Override
    public Future<?> shutdownGracefully() {
        if (!isShuttingDown()) {
            STATE_UPDATER.set(this, ST_SHUTTING_DOWN);
            if (wakeUpForTask) {
                wakeup(inExecutorThread());
            }
        }

        return terminationFuture;
    }

    @Override
    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    public boolean isShuttingDown() {
        return STATE_UPDATER.get(this) >= ST_SHUTTING_DOWN;
    }

    @Override
    public boolean isShutdown() {
        return STATE_UPDATER.get(this) >= ST_SHUTDOWN;
    }

    @Override
    public boolean isTerminated() {
        return STATE_UPDATER.get(this) == ST_TERMINATED;
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

        if (wakeUpForTask && wakeUpForTask(command)) {
            wakeup(inExecutorThread());
        }
    }

    protected boolean wakeUpForTask(Runnable task) {
        return true;
    }

    protected void wakeup(boolean inExecutorThread) {
        if (!inExecutorThread) {
            taskQueue.add(WAKEUP_TASK);
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
                SingleThreadEventExecutor.this.thread = Thread.currentThread();

                boolean success = false;
                try {
                    SingleThreadEventExecutor.this.run();
                    success = true;
                } catch (Throwable e) {
                    logger.warn("Unexpected exception from an event executor: ", e);
                }

                for (; ; ) {
                    int oldState = STATE_UPDATER.get(SingleThreadEventExecutor.this);
                    if (oldState >= ST_SHUTTING_DOWN || STATE_UPDATER.compareAndSet(SingleThreadEventExecutor.this, oldState, ST_SHUTTING_DOWN)) {
                        break;
                    }
                }

                if (success && !shutdownConfirmed) {
                    logger.error("Buggy " + EventExecutor.class.getSimpleName() + " implementation; " +
                            SingleThreadEventExecutor.class.getSimpleName() + ".confirmShutdown() must be called " +
                            "before run() implementation terminates.");
                }

                try {
                    for (; ; ) {
                        if (confirmShutdown()) {
                            break;
                        }
                    }
                } finally {
                    STATE_UPDATER.set(SingleThreadEventExecutor.this, ST_TERMINATED);
                    terminationFuture.setSuccess(null);
                }
            }
        });
    }

    protected boolean confirmShutdown() {
        if (!inExecutorThread()) {
            throw new IllegalStateException("must be invoked from executor thread");
        }

        if (!isShuttingDown()) {
            return false;
        }

        cancelDelayedTasks();
        shutdownConfirmed = true;

        if (runAllTasks()) {
            if (isShutdown()) {
                return true;
            }
        }

        return !hasTasks() || isShutdown();
    }

    protected abstract void run();
}
