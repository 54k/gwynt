package io.gwynt.core.concurrent;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractEventExecutor extends AbstractExecutorService implements EventExecutor {

    private static final Runnable WAKEUP_TASK = new Runnable() {
        @Override
        public void run() {
            // Do nothing.
        }
    };

    private Thread thread;
    private Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private Queue<ScheduledFutureTask<?>> delayedTaskQueue = new PriorityQueue<>();
    private volatile boolean running;

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

    protected void fetchFromDelayedQueue() {
        long millisTime = 0L;
        for (; ; ) {
            ScheduledFutureTask<?> delayedTask = delayedTaskQueue.peek();
            if (delayedTask == null) {
                break;
            }

            if (millisTime == 0L) {
                millisTime = ScheduledFutureTask.timeMillis();
            }

            if (delayedTask.deadlineMillis() <= millisTime) {
                delayedTaskQueue.remove();
                taskQueue.add(delayedTask);
            } else {
                break;
            }
        }
    }

    protected boolean hasTasks() {
        return !taskQueue.isEmpty();
    }

    protected void addTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }
        if (taskQueue.add(task)) {
            if (task != WAKEUP_TASK) {
                taskAdded(task);
            }
        }
    }

    protected void taskAdded(Runnable task) {
    }

    protected void removeTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
        }
        if (taskQueue.remove(task)) {
            taskRemoved(task);
        }
    }

    protected void taskRemoved(Runnable task) {
    }

    @Override
    public boolean inExecutorThread() {
        return Thread.currentThread() == thread;
    }

    @Override
    public void shutdown() {
        if (!running) {
            return;
        }
        addTask(WAKEUP_TASK);
        running = false;
        thread = null;
    }

    @Deprecated
    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return !running;
    }

    @Override
    public boolean isTerminated() {
        return !running && !hasTasks();
    }

    @Deprecated
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(Runnable command) {
        addTask(command);
        if (!running) {
            runThread();
        }
    }

    protected long delayMillis(long currentTimeMillis) {
        ScheduledFutureTask<?> delayedTask = delayedTaskQueue.peek();
        if (delayedTask == null) {
            return 1;
        }

        return delayedTask.delayMillis(currentTimeMillis);
    }

    private void runThread() {
        if (running) {
            return;
        }
        running = true;
        scheduleAtFixedRate(new PurgeTask(), 0, 1, TimeUnit.MILLISECONDS);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                AbstractEventExecutor.this.run();
            }
        });
        thread.start();
        this.thread = thread;
    }

    protected abstract void run();

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new PromiseTask<>(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new PromiseTask<>(callable);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return (Future<?>) super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return (Future<T>) super.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return (Future<T>) super.submit(task);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, PromiseTask.toCallable(command), ScheduledFutureTask.deadlineMillis(unit.toMillis(delay)), delayedTaskQueue));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, callable, ScheduledFutureTask.deadlineMillis(unit.toMillis(delay)), delayedTaskQueue));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, PromiseTask.toCallable(command), ScheduledFutureTask.deadlineMillis(unit.toMillis(initialDelay)), unit.toMillis(period),
                delayedTaskQueue));
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, PromiseTask.toCallable(command), ScheduledFutureTask.deadlineMillis(unit.toMillis(initialDelay)), -unit.toMillis(delay),
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

    private class PurgeTask implements Runnable {

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
