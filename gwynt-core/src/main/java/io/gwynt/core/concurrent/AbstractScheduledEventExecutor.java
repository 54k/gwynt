package io.gwynt.core.concurrent;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractScheduledEventExecutor extends AbstractEventExecutor {

    private static final long PURGE_TASK_INTERVAL = TimeUnit.SECONDS.toNanos(5);

    private Queue<ScheduledFutureTask<?>> delayedTaskQueue = new PriorityQueue<>();

    protected AbstractScheduledEventExecutor() {
    }

    protected AbstractScheduledEventExecutor(EventExecutorGroup parent) {
        super(parent);
    }

    protected ScheduledFutureTask<?> fetchDelayedTask() {
        ScheduledFutureTask<?> delayedTask = delayedTaskQueue.peek();
        if (delayedTask == null) {
            return null;
        }

        long nanoTime = ScheduledFutureTask.nanos();

        if (delayedTask.triggerTime() <= nanoTime) {
            delayedTaskQueue.remove();
            return delayedTask;
        }
        return null;
    }

    protected ScheduledFutureTask<?> peekDelayedTask() {
        return delayedTaskQueue.peek();
    }

    protected void cancelDelayedTasks() {
        for (ScheduledFutureTask<?> scheduledFutureTask : delayedTaskQueue) {
            scheduledFutureTask.cancel();
        }
        delayedTaskQueue.clear();
    }

    protected int pendingTasks() {
        return delayedTaskQueue.size();
    }

    protected void schedulePurgeTask() {
        delayedTaskQueue.add(newPurgeTask());
    }

    private ScheduledFutureTask<?> newPurgeTask() {
        return new ScheduledFutureTask<>(AbstractScheduledEventExecutor.this, PromiseTask.toCallable(new PurgeTask()), ScheduledFutureTask.triggerTime(PURGE_TASK_INTERVAL),
                -PURGE_TASK_INTERVAL, delayedTaskQueue);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, PromiseTask.toCallable(command), ScheduledFutureTask.triggerTime(unit.toNanos(initialDelay)), -unit.toNanos(delay),
                delayedTaskQueue));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, PromiseTask.toCallable(command), ScheduledFutureTask.triggerTime(unit.toNanos(initialDelay)), unit.toNanos(period),
                delayedTaskQueue));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, callable, ScheduledFutureTask.triggerTime(unit.toNanos(delay)), delayedTaskQueue));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedule(new ScheduledFutureTask<>(this, PromiseTask.toCallable(command), ScheduledFutureTask.triggerTime(unit.toNanos(delay)), delayedTaskQueue));
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return super.newTaskFor(callable);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return super.newTaskFor(runnable, value);
    }

    private <V> ScheduledFuture<V> schedule(final ScheduledFutureTask<V> task) {
        if (task == null) {
            throw new IllegalArgumentException("task");
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

    private final class PurgeTask implements Runnable {
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
