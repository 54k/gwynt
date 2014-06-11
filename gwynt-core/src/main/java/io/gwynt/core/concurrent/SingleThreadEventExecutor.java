package io.gwynt.core.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public abstract class SingleThreadEventExecutor extends AbstractEventExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventExecutor.class);

    private static final int ST_NOT_STARTED = 1;
    private volatile int state = ST_NOT_STARTED;
    private static final int ST_STARTED = 2;
    private static final int ST_SHUTTING_DOWN = 3;
    private static final int ST_SHUTDOWN = 4;
    private static final int ST_TERMINATED = 5;
    private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");

    private static final Runnable WAKEUP_TASK = new Runnable() {
        @Override
        public void run() {
            // Do nothing.
        }
    };
    private final Promise<Void> shutdownPromise = new DefaultPromise<>();
    private final boolean wakeUpForTask;
    private final Executor executor;
    private int executedTasks = 0;
    private long lastExecutionTimeNanos;

    private Thread thread;
    private Queue<Runnable> taskQueue = newTaskQueue();
    private Queue<ScheduledFutureTask<?>> delayedTaskQueue = new PriorityQueue<>();

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
                if (deadlineNanos >= lastExecutionTimeNanos()) {
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
        ScheduledFutureTask<?> delayedTask = delayedTaskQueue.peek();
        if (delayedTask == null) {
            return -1;
        }

        return delayedTask.getDelayNanos(timeNanos);
    }

    protected Runnable takeTask() {
        if (this.taskQueue instanceof BlockingQueue) {
            return takeTask((BlockingQueue<Runnable>) this.taskQueue);
        }
        return pollTask();
    }

    private Runnable takeTask(BlockingQueue<Runnable> taskQueue) {
        for (; ; ) {
            ScheduledFutureTask<?> delayedTask = delayedTaskQueue.peek();

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
        long nanoTime = 0L;
        for (; ; ) {
            ScheduledFutureTask<?> delayedTask = delayedTaskQueue.peek();
            if (delayedTask == null) {
                break;
            }

            if (nanoTime == 0L) {
                nanoTime = ScheduledFutureTask.nanos();
            }

            if (delayedTask.triggerTime() <= nanoTime) {
                delayedTaskQueue.remove();
                taskQueue.add(delayedTask);
            } else {
                break;
            }
        }
    }

    protected Runnable peekTask() {
        for (; ; ) {
            Runnable task = taskQueue.peek();
            if (task == WAKEUP_TASK) {
                continue;
            }
            return task;
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

    private void cancelDelayedTasks() {
        for (ScheduledFutureTask<?> scheduledFutureTask : delayedTaskQueue) {
            scheduledFutureTask.cancel();
        }
        delayedTaskQueue.clear();
    }

    protected int pendingTasks() {
        return delayedTaskQueue.size();
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

        STATE_UPDATER.set(this, ST_NOT_STARTED);
        thread = null;
    }

    @Override
    public boolean isShutdown() {
        return state >= ST_SHUTTING_DOWN;
    }

    @Override
    public boolean isTerminated() {
        return isShutdown() && !hasTasks();
    }

    @Deprecated
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(Runnable command) {
        addTask(command);
        if (state == ST_NOT_STARTED) {
            startThread();
        }

        if (wakeUpForTask && wakeUpForTask(command)) {
            wakeup(inExecutorThread());
        }
    }

    @Override
    public boolean inExecutorThread(Thread thread) {
        return this.thread == thread;
    }

    protected boolean wakeUpForTask(Runnable task) {
        return true;
    }

    protected void wakeup(boolean inExecutorThread) {
        if (!inExecutorThread) {
            taskQueue.add(WAKEUP_TASK);
        }
    }

    private void startThread() {
        if (STATE_UPDATER.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
            delayedTaskQueue.add(new ScheduledFutureTask<>(SingleThreadEventExecutor.this, PromiseTask.toCallable(new PurgeTask()),
                    ScheduledFutureTask.triggerTime(TimeUnit.SECONDS.toNanos(5)), TimeUnit.SECONDS.toNanos(5), delayedTaskQueue));
            doStartThread();
        }
    }

    private void doStartThread() {
        assert thread == null;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                SingleThreadEventExecutor.this.thread = Thread.currentThread();

                try {
                    SingleThreadEventExecutor.this.run();
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }

                STATE_UPDATER.set(SingleThreadEventExecutor.this, ST_NOT_STARTED);
            }
        });
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, PromiseTask.toCallable(command), ScheduledFutureTask.triggerTime(unit.toNanos(delay)), delayedTaskQueue));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, callable, ScheduledFutureTask.triggerTime(unit.toNanos(delay)), delayedTaskQueue));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, PromiseTask.toCallable(command), ScheduledFutureTask.triggerTime(unit.toNanos(initialDelay)), unit.toNanos(period),
                delayedTaskQueue));
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, PromiseTask.toCallable(command), ScheduledFutureTask.triggerTime(unit.toNanos(initialDelay)), -unit.toNanos(delay),
                delayedTaskQueue));
    }

    private <V> ScheduledFuture<V> schedule(final ScheduledFutureTask<V> task) {
        if (task == null) {
            throw new NullPointerException("task");
        }

        if (inExecutorThread()) {
            delayedTaskQueue.add(task);
        } else {
            execute(new Runnable() {
                @Override
                public void run() {
                    delayedTaskQueue.add(task);
                }
            });
        }

        return task;
    }

    protected abstract void run();

    protected long updateLastExecutionTime() {
        return lastExecutionTimeNanos = System.nanoTime();
    }

    @Override
    public long lastExecutionTimeNanos() {
        return lastExecutionTimeNanos;
    }

    @Override
    public long lastExecutionTime(TimeUnit timeUnit) {
        return timeUnit.convert(lastExecutionTimeNanos, TimeUnit.NANOSECONDS);
    }

    protected class PurgeTask implements Runnable {

        @Override
        public void run() {
            Iterator<ScheduledFutureTask<?>> i = delayedTaskQueue.iterator();
            while (i.hasNext()) {
                ScheduledFutureTask<?> task = i.next();
                if (task.isCancelled()) {
                    i.remove();
                }
            }
        }
    }
}
